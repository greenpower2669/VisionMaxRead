package fr.visionmaxread;
import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
/** Bound allocation before decode, then normalize all EXIF rotations and mirrors. */
final class ImageLoader {
    interface Source { InputStream open() throws IOException; }
    static Bitmap fromUri(Context context,Uri uri) throws IOException {
        return load(()->{InputStream stream=context.getContentResolver().openInputStream(uri);if(stream==null)throw new IOException("Image inaccessible");return stream;});
    }
    static Bitmap fromJpeg(byte[] bytes) throws IOException {return load(()->new ByteArrayInputStream(bytes));}
    private static Bitmap load(Source source) throws IOException {
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
        try(InputStream in=source.open()){BitmapFactory.decodeStream(in,null,options);}
        if(options.outWidth<1||options.outHeight<1)throw new IOException("Format d’image non reconnu");
        options.inSampleSize=1;
        while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>2560)options.inSampleSize*=2;
        options.inJustDecodeBounds=false;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
        Bitmap image;try(InputStream in=source.open()){image=BitmapFactory.decodeStream(in,null,options);}
        if(image==null)throw new IOException("Décodage impossible");
        int orientation;
        try(InputStream in=source.open()){orientation=new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);}
        catch(IOException e){orientation=ExifInterface.ORIENTATION_NORMAL;}
        Matrix matrix=new Matrix();
        switch(orientation){
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:matrix.setScale(-1,1);break;
            case ExifInterface.ORIENTATION_ROTATE_180:matrix.setRotate(180);break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:matrix.setScale(1,-1);break;
            case ExifInterface.ORIENTATION_TRANSPOSE:matrix.setRotate(90);matrix.postScale(-1,1);break;
            case ExifInterface.ORIENTATION_ROTATE_90:matrix.setRotate(90);break;
            case ExifInterface.ORIENTATION_TRANSVERSE:matrix.setRotate(270);matrix.postScale(-1,1);break;
            case ExifInterface.ORIENTATION_ROTATE_270:matrix.setRotate(270);break;
            default:return image;
        }
        Bitmap rotated=Bitmap.createBitmap(image,0,0,image.getWidth(),image.getHeight(),matrix,true);if(rotated!=image)image.recycle();return rotated;
    }
}
