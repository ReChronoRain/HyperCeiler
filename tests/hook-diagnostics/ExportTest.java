import com.sevtinge.hyperceiler.common.diagnostics.DiagnosticExport;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class ExportTest {
    public static void main(String[] args)throws Exception{
        File cache=Files.createTempDirectory("hc-diagnostic-export-").toFile();
        try{
            String first=DiagnosticExport.stage(cache,"first immutable snapshot 中文");
            String second=DiagnosticExport.stage(cache,"newer generation");
            // Reconstructed consumer has only a saved token, no report/model instance.
            var out=new ByteArrayOutputStream();DiagnosticExport.copy(cache,first,out);
            assert out.toString(StandardCharsets.UTF_8).equals("first immutable snapshot 中文");
            out.reset();DiagnosticExport.copy(cache,second,out);
            assert out.toString(StandardCharsets.UTF_8).equals("newer generation");
            DiagnosticExport.discard(cache,first);
            boolean missing=false;try{DiagnosticExport.copy(cache,first,out);}catch(IOException e){missing=true;}assert missing;
            boolean traversal=false;try{DiagnosticExport.copy(cache,"../private.txt",out);}catch(IOException e){traversal=true;}assert traversal;
            for(int i=0;i<24;i++)DiagnosticExport.stage(cache,"bounded "+i);
            assert new File(cache,"hook-diagnostics-exports").list().length==8;
            boolean bounded=false;try{DiagnosticExport.stage(cache,"x".repeat(4*1024*1024+1));}catch(IOException e){bounded=true;}assert bounded;
            System.out.println("Shipping export snapshots PASS: recreated consumer, immutable generation, missing/cache traversal rejection, byte/file budgets");
        }finally{
            try(var files=Files.walk(cache.toPath())){for(var p:files.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
