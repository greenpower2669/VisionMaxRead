package fr.visionmaxread;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.*;
import android.os.*;
import android.util.Size;
import android.view.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
/** No frame-by-frame OCR. The TextureView uses a small stream; capture uses a separate JPEG stream. */
final class CameraController {
    interface Listener { void captured(byte[] jpeg,long document);void error(String message); }
    private final Context context;private final TextureView view;private final Listener listener;
    private final HandlerThread thread=new HandlerThread("Camera");private final Handler handler;
    private final AtomicLong generation=new AtomicLong();
    private final OrientationEventListener orientationListener;
    private CameraDevice device;private CameraCaptureSession session;private ImageReader reader;private Surface previewSurface;
    private long document;private int sensorRotation;private boolean front;private volatile boolean wanted;private Size previewSize;
    private int lastDisplayDegrees=-1;
    // Tracks whether a camera open was actually queued after TextureView became available.
    private volatile boolean openRequested;
    CameraController(Context context,TextureView view,Listener listener){
        this.context=context;this.view=view;this.listener=listener;thread.start();handler=new Handler(thread.getLooper());
        orientationListener=new OrientationEventListener(context){
            @Override public void onOrientationChanged(int orientation){
                if(!wanted)return;
                int degrees=displayRotationDegrees();
                if(degrees==lastDisplayDegrees)return;
                lastDisplayDegrees=degrees;
                Size size=previewSize;
                if(size!=null)transform(size,generation.get());
            }
        };
        view.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){
            public void onSurfaceTextureAvailable(SurfaceTexture surface,int w,int h){if(wanted)start(document);}
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface,int w,int h){Size size=previewSize;if(size!=null)transform(size,generation.get());}
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface){stop();return true;}
            public void onSurfaceTextureUpdated(SurfaceTexture surface){}
        });
    }
    void start(long document){
        // Both MainActivity and TextureView may request the same document.
        // If TextureView was initially unavailable, allow the second callback
        // to schedule its *first* open instead of silently dropping it.
        boolean sameRequest=wanted&&this.document==document;
        if(sameRequest&&openRequested){
            DebugLog.append(context,"CAM duplicate start ignored document="+document);
            return;
        }
        if(!sameRequest){
            this.document=document;
            wanted=true;
            openRequested=false;
            lastDisplayDegrees=displayRotationDegrees();
            DebugLog.append(context,"CAM start displayDeg="+lastDisplayDegrees);
            if(orientationListener.canDetectOrientation())orientationListener.enable();
            generation.incrementAndGet();
        }
        if(view.isAvailable()&&!openRequested){
            openRequested=true;
            long ticket=generation.get();
            handler.post(()->open(ticket,document));
        }
    }
    void stop(){wanted=false;openRequested=false;orientationListener.disable();generation.incrementAndGet();handler.post(this::closeCamera);}
    void destroy(){orientationListener.disable();stop();handler.post(thread::quitSafely);}
    private boolean current(long ticket){return wanted&&generation.get()==ticket;}
    private void open(long ticket,long document){
        closeCamera();if(!current(ticket)||context.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;
        try{
            CameraManager manager=(CameraManager)context.getSystemService(Context.CAMERA_SERVICE);String chosen=null;
            for(String id:manager.getCameraIdList()){if(chosen==null)chosen=id;if(Objects.equals(manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING),CameraCharacteristics.LENS_FACING_BACK)){chosen=id;break;}}
            if(chosen==null)throw new IllegalStateException();CameraCharacteristics info=manager.getCameraCharacteristics(chosen);
            front=Objects.equals(info.get(CameraCharacteristics.LENS_FACING),CameraCharacteristics.LENS_FACING_FRONT);
            Integer angle=info.get(CameraCharacteristics.SENSOR_ORIENTATION);sensorRotation=angle==null?90:angle;
            DebugLog.append(context,"CAM open id="+chosen+" front="+front+" sensorDeg="+sensorRotation+" displayDeg="+displayRotationDegrees());
            StreamConfigurationMap map=info.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);if(map==null)throw new IllegalStateException();
            Size preview=choosePreview(map.getOutputSizes(SurfaceTexture.class));previewSize=preview;Size still=chooseStill(map.getOutputSizes(ImageFormat.JPEG));
            DebugLog.append(context,"CAM sizes preview="+preview.getWidth()+"x"+preview.getHeight()+" still="+still.getWidth()+"x"+still.getHeight());
            SurfaceTexture surface=view.getSurfaceTexture();if(surface==null)return;surface.setDefaultBufferSize(preview.getWidth(),preview.getHeight());
            previewSurface=new Surface(surface);transform(preview,ticket);
            reader=ImageReader.newInstance(still.getWidth(),still.getHeight(),ImageFormat.JPEG,2);
            reader.setOnImageAvailableListener(source->{
                try(Image image=source.acquireLatestImage()){
                    if(image==null||!current(ticket))return;
                    ByteBuffer buffer=image.getPlanes()[0].getBuffer();byte[] bytes=new byte[buffer.remaining()];buffer.get(bytes);
                    wanted=false;orientationListener.disable();listener.captured(bytes,document);
                }catch(Exception e){if(current(ticket))listener.error("La photo n’a pas pu être capturée.");}
                finally{closeCamera();}
            },handler);
            manager.openCamera(chosen,new CameraDevice.StateCallback(){
                public void onOpened(CameraDevice camera){if(!current(ticket)){camera.close();return;}device=camera;configure(ticket);}
                public void onDisconnected(CameraDevice camera){camera.close();if(current(ticket)){closeCamera();listener.error("Caméra déconnectée.");}}
                public void onError(CameraDevice camera,int error){camera.close();if(current(ticket)){closeCamera();listener.error("Caméra indisponible. Tu peux importer une photo.");}}
            },handler);
        }catch(Exception e){closeCamera();if(current(ticket))listener.error("Caméra indisponible. Vérifie l’autorisation caméra.");}
    }
    private void configure(long ticket){
        try{device.createCaptureSession(Arrays.asList(previewSurface,reader.getSurface()),new CameraCaptureSession.StateCallback(){
            public void onConfigured(CameraCaptureSession configured){
                if(!current(ticket)){configured.close();return;}session=configured;
                try{CaptureRequest.Builder request=device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);request.addTarget(previewSurface);request.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);session.setRepeatingRequest(request.build(),null,handler);}
                catch(CameraAccessException e){closeCamera();listener.error("Aperçu caméra indisponible.");}
            }
            public void onConfigureFailed(CameraCaptureSession failed){failed.close();if(current(ticket)){closeCamera();listener.error("Configuration caméra impossible.");}}
        },handler);}catch(CameraAccessException e){closeCamera();listener.error("Caméra indisponible.");}
    }
    void freeze(){long ticket=generation.get();handler.post(()->{
        if(!current(ticket)||device==null||session==null||reader==null){listener.error("Attends que l’aperçu soit prêt.");return;}
        try{
            CaptureRequest.Builder capture=device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);capture.addTarget(reader.getSurface());
            capture.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            int jpegDeg=jpegOrientation();DebugLog.append(context,"CAM freeze jpegDeg="+jpegDeg+" displayDeg="+displayRotationDegrees()+" sensorDeg="+sensorRotation);capture.set(CaptureRequest.JPEG_ORIENTATION,jpegDeg);capture.set(CaptureRequest.JPEG_QUALITY,(byte)95);
            session.stopRepeating();session.capture(capture.build(),new CameraCaptureSession.CaptureCallback(){
                public void onCaptureFailed(CameraCaptureSession s,CaptureRequest r,CaptureFailure failure){if(current(ticket)){closeCamera();listener.error("Capture échouée. Relance la caméra.");}}
            },handler);
        }catch(Exception e){closeCamera();listener.error("Capture impossible. Relance la caméra.");}
    });}
    private void closeCamera(){if(session!=null){session.close();session=null;}if(device!=null){device.close();device=null;}if(reader!=null){reader.close();reader=null;}if(previewSurface!=null){previewSurface.release();previewSurface=null;}previewSize=null;}
    private static Size choosePreview(Size[] sizes){
        if(sizes==null||sizes.length==0)throw new IllegalStateException();
        return Arrays.stream(sizes).min(Comparator.comparingLong(s->Math.abs((long)s.getWidth()*s.getHeight()-640L*480)+((s.getWidth()>1280)?10000000:0))).orElse(sizes[0]);
    }
    private static Size chooseStill(Size[] sizes){
        if(sizes==null||sizes.length==0)throw new IllegalStateException();
        return Arrays.stream(sizes).filter(s->(long)s.getWidth()*s.getHeight()<=12000000).max(Comparator.comparingLong(s->(long)s.getWidth()*s.getHeight())).orElseGet(()->Arrays.stream(sizes).min(Comparator.comparingLong(s->(long)s.getWidth()*s.getHeight())).get());
    }
    private int displayRotationDegrees(){
        Display display=view.getDisplay();
        int r=display!=null?display.getRotation():((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        switch(r){case Surface.ROTATION_90:return 90;case Surface.ROTATION_180:return 180;case Surface.ROTATION_270:return 270;default:return 0;}
    }
    /**
     * JPEG orientation is already proven correct on the Samsung test device.
     * Keep this capture path independent from the TextureView preview transform.
     */
    private int jpegOrientation(){
        int display=displayRotationDegrees();
        int sign=front?-1:1;
        return (sensorRotation-display*sign+360)%360;
    }
    private int displayRotationConstant(){
        Display display=view.getDisplay();
        return display!=null?display.getRotation():((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
    }
    /**
     * TextureView already applies the camera sensor orientation. Only compensate
     * the display rotation here, following the Camera2Basic TextureView pattern.
     * This deliberately does NOT reuse JPEG/sensor rotation.
     */
    private void transform(Size size,long ticket){view.post(()->{
        if(!current(ticket)||view.getWidth()==0||view.getHeight()==0)return;
        int surfaceRotation=displayRotationConstant();
        lastDisplayDegrees=displayRotationDegrees();
        float w=view.getWidth(),h=view.getHeight(),cx=w/2f,cy=h/2f;
        Matrix m=new Matrix();
        RectF viewRect=new RectF(0,0,w,h);
        int appliedDegrees=0;
        if(surfaceRotation==Surface.ROTATION_90||surfaceRotation==Surface.ROTATION_270){
            RectF bufferRect=new RectF(0,0,size.getHeight(),size.getWidth());
            bufferRect.offset(cx-bufferRect.centerX(),cy-bufferRect.centerY());
            m.setRectToRect(viewRect,bufferRect,Matrix.ScaleToFit.FILL);
            float scale=Math.max(h/(float)size.getHeight(),w/(float)size.getWidth());
            m.postScale(scale,scale,cx,cy);
            appliedDegrees=surfaceRotation==Surface.ROTATION_90?-90:90;
            m.postRotate(appliedDegrees,cx,cy);
        }else if(surfaceRotation==Surface.ROTATION_180){
            appliedDegrees=180;
            m.postRotate(180,cx,cy);
        }
        if(front)m.postScale(-1,1,cx,cy);
        DebugLog.append(context,"CAM transform view="+Math.round(w)+"x"+Math.round(h)+
            " buffer="+size.getWidth()+"x"+size.getHeight()+
            " matrixDeg="+appliedDegrees+
            " displayDeg="+lastDisplayDegrees+
            " sensorDeg="+sensorRotation+
            " front="+front);
        view.setTransform(m);
    });}
}
