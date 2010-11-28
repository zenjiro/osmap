import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * ネットワーク関係のユーティリティクラス
 */
public class WebUtilities {
    /**
     * ストリームを使ってファイルをコピーします。
     * @param in 入力ストリーム
     * @param out 出力ストリーム
     * @throws IOException 入出力例外 
     */
    public static void copy(final InputStream in, final OutputStream out) throws IOException {
        final byte buf[] = new byte[1024];
        int size;
        while ((size = in.read(buf)) != -1) {
            out.write(buf, 0, size);
        }
        out.close();
    }
}
