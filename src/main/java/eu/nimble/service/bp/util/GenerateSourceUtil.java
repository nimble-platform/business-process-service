package eu.nimble.service.bp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateSourceUtil {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    final private String regex_process_instance_group_dao = "public class ProcessInstanceGroupDAO";
    final private String regex_process_instance_groups_process_ids = "public List<ProcessInstanceGroupDAO.ProcessInstanceGroupDAOProcessInstanceIDsItem> getProcessInstanceIDsItems()";
    final private String regex_import_statement = "import";

    final private String import_statement_for_order_by = "import javax.persistence.OrderBy;\n";
    final private String order_by_annotation = "@OrderBy(\"hjid\")\n    ";

    public static void main(String [] args){
        GenerateSourceUtil generateSourceUtil = new GenerateSourceUtil();
        generateSourceUtil.postProcessORMAnnotations(args[0]);
    }

    public void postProcessORMAnnotations(String path){
        logger.debug("Started to process ORM annotations");
        File directory = new File(path);
        searchDirectory(directory);
        logger.debug("Process ORM annotations successfully");
    }

    public void searchDirectory(File directory){
        File[] filesAndDirs = directory.listFiles();
        for(File file : filesAndDirs){
            if(file.isFile()){
                String fileContent = getFileContent(file);
                FileUpdate fileUpdate = new FileUpdate();
                fileUpdate.setContent(fileContent);

                addOrderByAnnotation(fileUpdate);

                updateFile(file, fileUpdate);
            }
            else {
                searchDirectory(file);
            }
        }
    }

    private void addOrderByAnnotation(FileUpdate fileUpdate) {
        String fileContent = fileUpdate.getContent();
        Pattern p = Pattern.compile(regex_process_instance_group_dao,Pattern.DOTALL);
        Matcher m = p.matcher(fileContent);
        if(m.find()){
            fileContent = fileContent.replace(regex_process_instance_groups_process_ids, order_by_annotation+regex_process_instance_groups_process_ids);
            fileContent = fileContent.replaceFirst(regex_import_statement,import_statement_for_order_by+regex_import_statement);
            fileUpdate.setContent(fileContent);
            fileUpdate.setFileUpdated(true);
        }
    }

    private void updateFile(File file, FileUpdate fileUpdate) {
        if(!fileUpdate.isFileUpdated()) {
            return;
        }

        try {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(fileUpdate.getContent());

            BufferedWriter bwr = new BufferedWriter(new FileWriter(file));

            //write contents of StringBuffer to the file
            bwr.write(stringBuffer.toString());

            //flush the stream
            bwr.flush();

            //close the stream
            bwr.close();

        } catch(IOException e) {
            throw new RuntimeException("Failed to update file", e);
        }
    }

    private String getFileContent(File file) {
        FileInputStream fileStream = null;
        BufferedReader br = null;
        InputStreamReader inputStreamReader = null;
        try {
            StringBuffer text = new StringBuffer();
            fileStream = new FileInputStream(file);
            inputStreamReader = new InputStreamReader(fileStream);
            br = new BufferedReader(inputStreamReader);
            for (String line; (line = br.readLine()) != null; )
                text.append(line + System.lineSeparator());

            String fileText = text.toString();
            return fileText;

        } catch (IOException e) {
            throw new RuntimeException("Failed to get file content", e);
        } finally {
            if(fileStream != null){
                try {
                    fileStream.close();
                } catch (IOException e) {
                    logger.warn("Failed to close file stream: ",e);
                }
            }
            if(br != null){
                try {
                    br.close();
                } catch (IOException e) {
                    logger.warn("Failed to close buffered reader: ",e);
                }
            }
            if(inputStreamReader != null){
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    logger.warn("Failed to close input stream reader: ",e);
                }
            }
        }
    }

    static class FileUpdate {
        private boolean fileUpdated = false;
        private String content;

        public boolean isFileUpdated() {
            return fileUpdated;
        }

        public void setFileUpdated(boolean fileUpdated) {
            this.fileUpdated = fileUpdated;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
    
}
