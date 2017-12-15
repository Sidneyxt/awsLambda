package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sidney 2017/12/12.
 */
public class Handler implements RequestHandler<S3Event, String> {
    private static final int MIN_FRAME = 12;
    private final String MP4_TYPE = (String) "mp4";

    static BufferedImage myJpegImage=null;

    @Override
    public String handleRequest(S3Event s3event, Context context) {
        return getThumbnail(s3event);
    }

    public static void main(String[] args) {

        try {
            Handler handler = new Handler();
            File file = new File("/Users/chenxiaotian/Downloads/3zqIdhq762.mp4");

            handler.getFrameFromMp4(new FileInputStream(file), null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getFrameFromMp4(InputStream iis, String dstBucket, String key)
            throws Exception {
        FFmpegFrameGrabber g = new FFmpegFrameGrabber(iis);
        g.start();
//        CanvasFrame canvasFrame = new CanvasFrame("/Users/chenxiaotian/Documents/shell/preCN/Test");

        int length = g.getLengthInFrames();

        if (length < MIN_FRAME) {
            System.out.println("length :[" + length + "] is less than MIN_FRAME");
            return;
        }
        int interval = length / MIN_FRAME;
        int j = 0;
        for (int i = 0; i < length; i++) {
            if (g.grab() == null) {
                System.out.println("Frame is null");
            }
            org.bytedeco.javacv.Frame frame = g.grabImage();
            if (frame == null || frame.image == null) {
                System.out.println("null");
            }
//            canvasFrame.showImage(frame);
            if (i % interval == interval - 1) {
                doExecuteFrame(frame, j, dstBucket, key);
                j++;
            }
        }

        g.flush();
//        canvasFrame.dispose();
    }

    public static void doExecuteFrame(org.bytedeco.javacv.Frame f, int index, String dstBucket, String key)
            throws IOException {
        if (null == f || null == f.image) {
            return;
        }

        Java2DFrameConverter converter = new Java2DFrameConverter();

        String imageMat = "jpg";
        BufferedImage bi = converter.getBufferedImage(f);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(bi, imageMat, os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        // Set Content-Length and Content-Type
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(os.size());
        meta.setContentType(imageMat);
        AmazonS3 s3Client = new AmazonS3Client();
        String dstKey = key + "_" + index + "." + imageMat;
        s3Client.putObject(dstBucket, dstKey, is, meta);
        System.out.println("Successful writing to: " + dstBucket + "/" + dstKey);
    }


    private String getThumbnail(S3Event s3event) {
        try {
            S3EventNotification.S3EventNotificationRecord record = s3event.getRecords().get(0);

            String srcBucket = record.getS3().getBucket().getName();
            // Object key may have spaces or unicode non-ASCII characters.
            String srcKey = record.getS3().getObject().getKey()
                    .replace('+', ' ');
            srcKey = URLDecoder.decode(srcKey, "UTF-8");

            String dstBucket = srcBucket;

            // Infer the image type.
            Matcher matcher = Pattern.compile(".*\\.([^\\.]*)").matcher(srcKey);
            if (!matcher.matches()) {
                System.out.println("Unable to infer image type for key "
                        + srcKey);
                return "";
            }
            String imageType = matcher.group(1);
            if (!MP4_TYPE.equals(imageType)) {
                System.out.println("Skipping non-mp4 " + srcKey);
                return "";
            }

            // Download the image from S3 into a stream
            AmazonS3 s3Client = new AmazonS3Client();
            S3Object s3Object = s3Client.getObject(new GetObjectRequest(
                    srcBucket, srcKey));
            InputStream objectData = s3Object.getObjectContent();

            String dstKey = srcKey.replace(".mp4", "");

            try {
                getFrameFromMp4(objectData, dstBucket, dstKey);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return "Ok";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
