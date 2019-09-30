package dc.pay.utils;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FilesUtil {

    public  static  String  getFile(final String fileName) throws IOException {
        String contents="";
        File testFile = new File(fileName);
        List<String> lines = Files.readLines(testFile, Charsets.UTF_16);
        for (String line : lines) {
            contents+=line;
        }
        return contents;
    }


    public static void writeFile(final String fileName, final String contents)
    {
        final File newFile = new File(fileName);
        try{
            Files.write(contents.getBytes(), newFile);
        }catch (IOException fileIoEx)
        {
            System.err.println(  "ERROR trying to write to file '" + fileName + "' - "+ fileIoEx.toString());
        }
    }



}
