import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.apache.commons.lang3.StringEscapeUtils;

import javax.jws.soap.SOAPBinding;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by qtfreet on 2017/2/24.
 */
public class Main {
    static {

    }

    private static List<String> filelist = new ArrayList();

    public static void main(String[] args) throws UnsupportedEncodingException {
        System.out.println("请输入已反编译apk的路径：");
        Scanner scanner = new Scanner(System.in);
        String path = scanner.next();
        String packagePath = getPackagePath(path);
        String smaliPath = path + "\\smali\\" + packagePath;
        getFiles(smaliPath);
        if (filelist == null) {
            System.out.println("未发现smali文件");
            return;
        }
        int size = filelist.size();
        for (int i = 0; i < size; i++) {
            String signalFile = filelist.get(i);
            if (!signalFile.endsWith(".smali")) {
                continue;
            }
            int j = i + 1;
            int k = size - i;
            //      System.out.println("当前是第 " + j + "个文件，还剩" + k + "个");
            FileTofindString(signalFile);
        }
        inject(path + "\\smali\\com");
        System.out.println("任务完成");
        System.out.println("直接重打包apk即可");
    }

    private static void inject(String path) {
        try {
            File file = new File(path);
            if (!file.exists() || !file.isDirectory()) {
                file.mkdir();
            }
            InputStream resourceAsStream = Main.class.getResourceAsStream("/qtfreet00.smali");
            InputStreamReader read = new InputStreamReader(resourceAsStream);
            BufferedReader br = new BufferedReader(read);
            String str = "";
            StringBuilder sb = new StringBuilder();
            while ((str = br.readLine()) != null) {
                sb.append(str + "\n");
            }
            FileOutputStream fos = new FileOutputStream(new File(path + "\\com.OooOO0OO.smali"));
            fos.write(sb.toString().getBytes("UTF-8"));
            fos.flush();
            fos.close();
        } catch (Exception e) {

        }

    }

    private static String getPackagePath(String path) {
        try {
            String AndroidManifestPath = path + "\\AndroidManifest.xml";
            File file = new File(AndroidManifestPath);
            if (!file.exists()) {
                System.out.println("未在目录下发现AndroidManifest.xml文件");
                System.exit(0);
                return "";
            }
            String packgaName = "";
            InputStreamReader read = new InputStreamReader(new FileInputStream(file), "UTF-8");
            BufferedReader br = new BufferedReader(read);
            String str = "";
            while ((str = br.readLine()) != null) {
                Matcher m = Pattern.compile("package=\"([\\w\\.]+)\"").matcher(str);
                if (m.find()) {
                    packgaName = m.group(1);
                    break;
                }

            }
            return packgaName.replace(".", "\\");
        } catch (Exception e) {

        }
        return "";
    }


    /**
     * 匹配文件
     *
     * @param path 每个文件对应路径
     */
    private static void FileTofindString(String path) {
//        boolean hashConstructor = false;
//        HashMap<String, String> map = new HashMap<>();

        StringBuilder sb = new StringBuilder();
        try {
            InputStreamReader read = new InputStreamReader(new FileInputStream(path), "UTF-8");
            BufferedReader br = new BufferedReader(read);
            String str = "";

            while ((str = br.readLine()) != null) {
//                Matcher removeFinalString = Pattern.compile("(.field (public|private) static final (\\w+):Ljava/lang/String;) = \"(\\w*)\"").matcher(str);
//                if (removeFinalString.find()) {
//                    // group(1) 为.field public static final BUILD_TYPE:Ljava/lang/String;
//                    //group(3) 为变量名
//                    //group(4) 为变量值
//                    str = removeFinalString.group(1);
//                    map.put()
//                    continue;
//                }
                //利用正则去匹配方法中定义的字符串
                Matcher m = Pattern.compile("const-string ([vp]\\d{1,2}), \"(.*)\"").matcher(str);
                if (m.find()) {
                    String tmp = m.group(2);
                    if (tmp.equals("")) {
                        sb.append(str + "\n");
                        continue;
                    }
                    //字符串转义,过滤掉\（如\",不转义时获取到的为\"，但理想效果应为"）以及将smali中的unicode转为中文字符
                    tmp = StringEscapeUtils.unescapeJava(tmp);
                    String register = m.group(1);
                    //register代表寄存器
                    String enc = qtfreet00.encode(tmp);
                    //混淆字符串
                    String sign = "    const-string " + register + ", " + "\"" + enc + "\"";
                    String dec = "";
                    if (Integer.parseInt(register.substring(1)) > 15 && register.startsWith("v")) {
                        //此处考虑寄存器个数，如果v寄存器大于15时，应使用range方式传参
                        dec = "    invoke-static/range {" + register + " .. " + register + "}, Lcom/com.OooOO0OO;->OooOOoo0oo(Ljava/lang/String;)Ljava/lang/String;";
                        //添加解密方法
                    } else if (register.startsWith("v") || (register.startsWith("p") && Integer.parseInt(register.substring(1)) < 10)) {
                        //此处p在10以上（不清楚具体），也会出现一些问题，由于没太接触过较大p寄存器，这里直接忽略掉了10以上的，实际应用中也很少会出现
                        //p在方法中一般代表入参，静态方法中从p0开始，非静态方法从p1开始，p0带表this
                        dec = "    invoke-static {" + register + "}, Lcom/com.OooOO0OO;->OooOOoo0oo(Ljava/lang/String;)Ljava/lang/String;";
                    } else {
                        sb.append(str + "\n");
                        continue;
                    }
                    String mov = "    move-result-object " + register;
                    sb.append(sign + "\n\n");
                    sb.append(dec + "\n\n");
                    sb.append(mov + "\n");
                } else {
                    sb.append(str + "\n");
                }
            }
            br.close();
            read.close();
            //覆盖掉源文件
//            FileOutputStream fos = new FileOutputStream(new File(path));
//            fos.write(sb.toString().getBytes("UTF-8"));
//            fos.flush();
//            fos.close();

        } catch (Exception e) {
        }
    }

    /**
     * 遍历所有文件，添加到list中
     *
     * @param filePath 文件路径
     */
    private static void getFiles(String filePath) {
        File[] files = new File(filePath).listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                getFiles(file.getPath());
            } else {
                filelist.add(file.getPath());
            }
        }
    }
}

