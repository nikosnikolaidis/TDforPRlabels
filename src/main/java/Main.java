import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Main {

    public static String SonarQube="http://localhost:9000";
    public static String GitHubAPIauthorization="";

    public static List<String> givenLabels=new ArrayList<>();

    public static void main(String[] args) {

        String GitURL="";

        if(args.length==1){
            GitURL=args[0];
        }
        else if(args.length==2){
            GitURL=args[0];
            givenLabels= Arrays.stream(args[1].split(";")).collect(Collectors.toList());
        }
        else{
            return;
        }

        String GitURLAPI = GitURL.replace("https://github.com/","https://api.github.com/repos/");
        System.out.println(GitURLAPI);

        if (isWindows()) {
            try {
                //Change dir and then clone
                Process proc = Runtime.getRuntime().exec("cmd /c cd " +System.getProperty("user.dir")+ " && "
                        + "git clone " + GitURL + "");
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String inputLine;
                while ((inputLine = inputReader.readLine()) != null) {
                    System.out.println(inputLine);
                }
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    System.out.println(errorLine);
                }
                System.out.println("Clone DONE!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                ProcessBuilder pbuilder = new ProcessBuilder("bash", "-c",
                        "cd '" + System.getProperty("user.dir") + "' ; git clone " + GitURL + "");
                File err = new File("err.txt");
                pbuilder.redirectError(err);
                Process p = pbuilder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                BufferedReader reader_2 = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                String line_2;
                while ((line_2 = reader_2.readLine()) != null) {
                    System.out.println(line_2);
                }
                System.out.println("Clone DONE!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


            try {
                //Get All Sha
                int page=1;
                boolean hasMore=true;
                ArrayList<PR> allPRs = new ArrayList<>();

                //start to get all PRs from API
                do{
                    //for each page
                    System.out.println();
                    System.out.println("-------------------------------");
                    System.out.println("-------------------------------");
                    System.out.println("-------------------------------");
                    System.out.println("Git API page: "+page);
                    URL url = new URL(GitURLAPI+"/pulls?state=closed&per_page=100&page="+page);
                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer "+GitHubAPIauthorization);
                    conn.connect();
                    int responsecode = conn.getResponseCode();
                    if(responsecode != 200) {
                        System.err.println(responsecode);
                        break;
                    }
                    else{
                        //get response
                        Scanner sc = new Scanner(url.openStream());
                        String inline="";
                        while(sc.hasNext()){
                            inline+=sc.nextLine();
                        }
                        sc.close();

                        JSONParser parse = new JSONParser();
                        JSONArray jsonArr_1 = (JSONArray) parse.parse(inline);
                        for(int i=0; i<jsonArr_1.size(); i++) {
                            //for each PR
                            JSONObject jsonObj_1 = (JSONObject)jsonArr_1.get(i);
                            String id = jsonObj_1.get("url").toString();
                            //if it was merged
                            if(jsonObj_1.get("merge_commit_sha") != null) {
                                String mergedSHA = jsonObj_1.get("merge_commit_sha").toString();
                                JSONArray jsonArr_2 = (JSONArray) jsonObj_1.get("labels");
                                //if it has >0 labels
                                if (jsonArr_2.size() > 0) {
                                    ArrayList<String> labels = new ArrayList<>();
                                    for (int k = 0; k < jsonArr_2.size(); k++) {
                                        JSONObject jsonObj_2 = (JSONObject) jsonArr_2.get(k);
                                        String labelName = jsonObj_2.get("name").toString();
                                        labels.add(labelName);
                                    }
                                    boolean shouldContinue= true;
                                    if(!givenLabels.isEmpty()) {
                                        shouldContinue = false;
                                        for (String s : labels) {
                                            if (givenLabels.contains(s)) {
                                                shouldContinue = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (shouldContinue) {
                                        //create PR
                                        PR pr = new PR(GitURL, id, mergedSHA, labels);
                                        //if it has previous SHA
                                        if (pr.getPreviousSha() != null) {
                                            //Make the 2 analysis of this PR
                                            pr.calculateTDofBothCommits();
                                            //Save this PR
                                            System.out.println("---------");
                                            System.out.println(pr);
                                            System.out.println("page: " + page + "     pr:" + (i + 1) + "/100");
                                            System.out.println("---------");
                                            allPRs.add(pr);
                                        }
                                    }
                                }
                            }
                        }
                        if(jsonArr_1.size()==0){
                            hasMore=false;
                        }
                        page++;

                        //write till here for safety
                        //save all PRs
                        File file = new File(System.getProperty("user.dir")+"/data-mid.csv");
                        file.delete();

                        FileWriter writer = new FileWriter(new File(System.getProperty("user.dir")+"/data-mid.csv"));
                        writer.write("projectName;id;previousSHA;mergedSHA;labels;tdPrevious;tdMerged;ccPrevious;ccMerged;locPrevious;locMerged;difTD;difCC"+
                                System.lineSeparator());
                        for(PR pr: allPRs){
                            writer.write(pr.projectName +";"+ pr.id +";"+ pr.previousSha +";"+ pr.mergedSha +";["+
                                    pr.label.stream().map(Object::toString).collect(Collectors.joining(",")).toString() +"];"+
                                    pr.sqaleIndexPrevious +";"+ pr.sqaleIndexMerged +";"+ pr.complexityPrevious +";"+ pr.complexityMerged +";"+
                                    pr.locPrevious +";"+ pr.locMerged +";"+ (1.0*(pr.sqaleIndexMerged/pr.locMerged) - 1.0*(pr.sqaleIndexPrevious/pr.locPrevious)) +";"+
                                    (1.0*(pr.complexityMerged/pr.locMerged) - 1.0*(pr.complexityPrevious/pr.locPrevious)) +System.lineSeparator());
                        }
                        writer.close();
                    }
                }while (hasMore);

                System.out.println("Got all PRs, now saving...");

                //save all PRs
                FileWriter writer = new FileWriter(new File(System.getProperty("user.dir")+"/data.csv"));
                writer.write("projectName;id;previousSHA;mergedSHA;labels;tdPrevious;tdMerged;ccPrevious;ccMerged;locPrevious;locMerged;difTD;difCC"+
                        System.lineSeparator());
                for(PR pr: allPRs){
                    writer.write(pr.projectName +";"+ pr.id +";"+ pr.previousSha +";"+ pr.mergedSha +";["+
                            pr.label.stream().map(Object::toString).collect(Collectors.joining(",")).toString() +"];"+
                            pr.sqaleIndexPrevious +";"+ pr.sqaleIndexMerged +";"+ pr.complexityPrevious +";"+ pr.complexityMerged +";"+
                            pr.locPrevious +";"+ pr.locMerged +";"+ (1.0*(pr.sqaleIndexMerged/pr.locMerged) - 1.0*(pr.sqaleIndexPrevious/pr.locPrevious)) +";"+
                            (1.0*(pr.complexityMerged/pr.locMerged) - 1.0*(pr.complexityPrevious/pr.locPrevious)) +System.lineSeparator());
                }
                writer.close();
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }

    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

}
