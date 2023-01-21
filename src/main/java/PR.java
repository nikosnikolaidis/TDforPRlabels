import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class PR {
    String projectOwner;
    String projectName;
    String id;
    String previousSha;
    String mergedSha;
    List<String> label;
    Integer sqaleIndexMerged;
    Integer sqaleIndexPrevious;
    Integer complexityMerged;
    Integer complexityPrevious;
    Integer locMerged;
    Integer locPrevious;


    public PR(String GitURL, String id, String mergedSha, List<String> label) {
        this.projectName = GitURL.split("/")[GitURL.split("/").length-1].replace(".git","");
        this.projectOwner = GitURL.split("/")[GitURL.split("/").length-2];
        this.id = id;
        this.mergedSha = mergedSha;
        this.label = label;

        //Get previous SHA
        findPreviousSha(mergedSha);
    }

    public void calculateTDofBothCommits(){
        try {
            String idOnlyPRNumber = id.split("/")[id.split("/").length-1];
            SonarAnalysis sonarAnalysisPrevious= new SonarAnalysis(projectOwner,projectName,previousSha,idOnlyPRNumber+".Previous");
            sqaleIndexPrevious= sonarAnalysisPrevious.getTD();
            complexityPrevious= sonarAnalysisPrevious.getComplexity();
            locPrevious= sonarAnalysisPrevious.getLOC();

            SonarAnalysis sonarAnalysisMerged= new SonarAnalysis(projectOwner,projectName,mergedSha,idOnlyPRNumber+".Merged");
            sqaleIndexMerged= sonarAnalysisMerged.getTD();
            complexityMerged= sonarAnalysisMerged.getComplexity();
            locMerged= sonarAnalysisMerged.getLOC();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void findPreviousSha(String mergedSha) {
        if (Main.isWindows()) {
            try {
                Process proc = Runtime.getRuntime().exec("cmd /c cd " +System.getProperty("user.dir")+ "\\" + projectName +
                        " && git rev-list --parents -n 1 " + mergedSha + "");
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String inputLine;
                while ((inputLine = inputReader.readLine()) != null) {
                    System.out.println(inputLine);
                    previousSha = inputLine.replace(mergedSha + " ", "");
                }
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    System.out.println(errorLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            try {
                ProcessBuilder pbuilder = new ProcessBuilder("bash", "-c",
                        "cd '" + System.getProperty("user.dir") +"/"+ projectName+"' ; git rev-list --parents -n 1 " +mergedSha);
                File err = new File("err.txt");
                pbuilder.redirectError(err);
                Process p = pbuilder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    previousSha = line.replace(mergedSha + " ", "");
                }
                BufferedReader reader_2 = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                String line_2;
                while ((line_2 = reader_2.readLine()) != null) {
                    System.out.println(line_2);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getProjectName() {
        return projectName;
    }

    public String getId() {
        return id;
    }

    public Integer getSqaleIndexMerged() {
        return sqaleIndexMerged;
    }

    public Integer getSqaleIndexPrevious() {
        return sqaleIndexPrevious;
    }

    public String getProjectOwner() {
        return projectOwner;
    }

    public Integer getComplexityMerged() {
        return complexityMerged;
    }

    public Integer getComplexityPrevious() {
        return complexityPrevious;
    }

    public String getPreviousSha() {
        return previousSha;
    }

    public void setPreviousSha(String previousSha) {
        this.previousSha = previousSha;
    }

    public String getMergedSha() {
        return mergedSha;
    }

    public void setMergedSha(String mergedSha) {
        this.mergedSha = mergedSha;
    }

    public List<String> getLabel() {
        return label;
    }

    public void setLabel(List<String> label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "PR{" +
                "projectName='" + projectName + '\'' +
                ", id='" + id + '\'' +
                ", sqaleIndexPrevious='" + sqaleIndexPrevious + '\'' +
                ", sqaleIndexMerged='" + sqaleIndexMerged + '\'' +
                ", label=" + label +
                '}';
    }
}
