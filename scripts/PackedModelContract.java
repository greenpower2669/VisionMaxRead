package fr.visionmaxread;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
public final class PackedModelContract {
    public static void main(String[] args) throws Exception {
        byte[] archive=Files.readAllBytes(Path.of(args[0]));
        if(args[0].endsWith(".base64"))archive=Base64.getMimeDecoder().decode(archive);
        MessageDigest hash=MessageDigest.getInstance("SHA-256");long size=0;
        try(InputStream input=ModelArchive.open(new ByteArrayInputStream(archive),true)){
            byte[] buffer=new byte[65536];int n;while((n=input.read(buffer))!=-1){hash.update(buffer,0,n);size+=n;}
        }
        StringBuilder hex=new StringBuilder();for(byte b:hash.digest())hex.append(String.format(Locale.ROOT,"%02x",b&255));
        if(size!=Long.parseLong(args[1])||!hex.toString().equals(args[2]))throw new AssertionError("Decompressed ONNX bytes must match the immutable model");
        boolean rejected=false;try(InputStream input=ModelArchive.open(new ByteArrayInputStream(Arrays.copyOf(archive,archive.length-30)),true)){
            byte[] b=new byte[65536];while(input.read(b)!=-1){}
        }catch(IOException e){rejected=true;}
        if(!rejected)throw new AssertionError("Truncated archive must fail closed");
        System.out.println("PASS lossless Java unpacking/hash and truncated archive rejection ("+size+" bytes)");
    }
}
