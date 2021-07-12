package it.aslcn1.ssde;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

public class ComputeSSDEForSeries {

    public static void main(String[] args) {


        File fileData = new File("D:\\DW\\dataPz.txt");
        FileWriter fr = null;
        BufferedWriter br = null;
        boolean isHead = false;
        try {
            for (String s : args) {
//                System.out.println(s);
            }
            //IL PATH SARA' QUELLO CONTENENTE LA  SERIE

            String pathString = args[0];

            //SORT BY IMAGENUMBER AND COPY TO "SORTED" DIRECTORY
            Utils.sortDirectoryByImageNumber(pathString);
            pathString+="/sorted";
            //LET'S WORK IN THE SORTED DIRECTORY
            Path workingDir = Paths.get(pathString);
//            System.err.println("workingDir: "+pathString);
            int numImmagini = 0;

            List<String> listaNomi = new ArrayList<>();
            try ( DirectoryStream<Path> stream = Files.newDirectoryStream(workingDir)) {

                for (Path file : stream) {

                    listaNomi.add(file.getFileName().toString());

                    numImmagini++;
                }
            } catch (IOException | DirectoryIteratorException x) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(x);
            }
            int fettaMediana = numImmagini/2; //CENTRAL SLICE
            if(fettaMediana==0){fettaMediana=1;} // A SERIES WITH JUST ONE IMAGE

            //BUILD COMPARATOR TO SORT THE IMAGES BY IMAGENUMBER
            Comparator<String> compareByImageNumber = new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    Double d1 = Double.parseDouble(o1);
                    Double d2 = Double.parseDouble(o2);
                    return d1.compareTo(d2);
                }
            };

            Collections.sort(listaNomi,compareByImageNumber);


            Opener opener = new Opener();


            //OPENENING THE CENTRAL SLICE TO ACQUIRE INFO
            ImagePlus imp = opener.openImage(pathString, listaNomi.get(fettaMediana-1));

            String accNumber = Utils.ExtractInfo(imp.getInfoProperty(), "Accession Number:", "\n");
            String protocol = Utils.ExtractInfo(imp.getInfoProperty(), "Protocol Name:", "\n");
            String sex = Utils.ExtractInfo(imp.getInfoProperty(), "Patient's Sex:", "\n");
            String age = Utils.ExtractInfo(imp.getInfoProperty(), "Patient's Age:", "\n");
            //REMOVE Y FROM AGE
            age = StringUtils.chop(age);

            //GETTING INFO ON PROTOCOL (DEPENDS ON PROTOCOL NAMES)

            if(protocol.isEmpty()){
                protocol = "NONE";
            }
            else if (protocol.toUpperCase().contains("TESTA") || protocol.toUpperCase().contains("HEAD") || protocol.toUpperCase().contains("NECK")
                    || protocol.toUpperCase().contains("COLLO") || protocol.toUpperCase().contains("CRANIO") || protocol.toUpperCase().contains("ENCEFALO")
                    || protocol.toUpperCase().contains("BRAIN")){
                isHead = true;
            }
            String stationName = Utils.ExtractInfo(imp.getInfoProperty(), "Station Name:", "\n");
            String header = imp.getInfoProperty();
            String seriesNumber = Utils.ExtractInfo(header,"Series Number:","\n");

            double waterEq[] =Utils.getWaterEquivalentInfoFromCTImage(imp);
            double areaMiddle = waterEq[1];
            double percentageTruncation = waterEq[2];

            double Dw=0d;
            double ssde=0.0;
            double ctdiFromHeader = 0.0;


            //STARTING THE LOOP OVER THE SLICES
            for(String nomeFile : listaNomi)
            {
                //System.err.println("PROVO A APRIRE IL FILE "+nomeFile);
                Instant start = Instant.now();
                opener = new Opener();
                imp = opener.openImage(pathString, nomeFile);
                //GET CTDI FROM DICOM HEADER




                //GET WATER EQIUVALENT DIAMETER INFOS
                waterEq = Utils.getWaterEquivalentInfoFromCTImage(imp);
                if(Double.isNaN(waterEq[0])){
                    //PROBLEM IN FINDING ROIS
                    continue;
                }
                //COMPUTE DW ONLY FOR ROIS GREATER THAN 90% OF CENTRAL IMAGES
                //MAYBE IT COULD BE CHANGED FOR EXTREMITIES
//                if(waterEq[1]>0.9*areaMiddle)
                if(true) //compute always on every slice
                {
                    Dw = waterEq[0];
                    header = imp.getInfoProperty();
                    String sCTDIvol = Utils.ExtractInfoFromTag(header, "0018,9345"); //GET THE CTDIvol FROM HEADER
                    if(!sCTDIvol.isEmpty())
                    {
                        ctdiFromHeader = Double.parseDouble(sCTDIvol);

                    }
                    else
                    {
                        ctdiFromHeader = Double.NaN;
                    }


                    double ssdeSlice = 0.0;
                    if(!isHead){
                        //HERE THE CTDI IS NOT FROM HEAD PHANTOM
                        //CHECK Reconstruction Diameter
                        String reconDiamS=Utils.ExtractInfo(imp.getInfoProperty(), "Reconstruction Diameter:", "\n");
                        double reconDiam = 400.0; //typical body diam FOV
                        if(!reconDiamS.isEmpty())
                        {
                            reconDiam = Double.parseDouble(reconDiamS);
                        }
                        if(reconDiam<250){
                            ssdeSlice = Double.NaN; //Dw NOT VALID FOR SSDE COMPUTATION
                        }
                        else if(reconDiam<=320)
                        {
                            ssdeSlice =ctdiFromHeader* 1.877*Math.exp(-0.039*waterEq[0]);
                        }
                        else
                        {
                            ssdeSlice =ctdiFromHeader* 3.7055*Math.exp(-0.037*waterEq[0]);
                        }


//                           System.err.println(IJ.d2s(waterEq[0],2)+"\t"+IJ.d2s(fattore,2));
                    }
                    else
                    {
                        //HERE WE HAVE CTDI HEAD
                        ssdeSlice =ctdiFromHeader* 1.9852*Math.exp(-0.0486*waterEq[0]);
//                           System.err.println(fattore);
                    }



                    percentageTruncation = waterEq[2]; //PERCENTAGE OF TRUNCATION OF THE SLICE
//                       System.err.println("PERC TRUNC: "+IJ.d2s(waterEq[2],2));

                    Instant finish = Instant.now();
                    long timeElapsed = Duration.between(start, finish).toMillis();  //in millis
                    try {
                        // to append to file, you need to initialize FileWriter using below constructor
                        fr = new FileWriter(fileData, true);
                        br = new BufferedWriter(fr);
                        br.newLine();

                        String text = accNumber+"\t"+seriesNumber+"\t"+nomeFile
                                +"\t"+protocol+"\t"+stationName+"\t"+IJ.d2s(Dw,1);
                        text+="\t"+IJ.d2s(ssdeSlice,3);
                        text+="\t"+IJ.d2s(ctdiFromHeader,3);
                        text+="\t"+IJ.d2s(percentageTruncation,1);
                        text+="\t"+ timeElapsed;
                        System.err.println("TIME FOR THIS IMAGE: "+timeElapsed);

                        text+="\t"+sex+"\t"+StringUtils.stripStart(age, "0");
                        br.write(text);


                    } catch (IOException e) {
                        e.printStackTrace();

                    } finally {
                        try {
                            br.close();
                            fr.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }


                }//ENDIF ON ROIS with Area>0.9 OF CENTRAL ROI Area
                //WRITING ON TEXT FILE (COULD BE CHANGED FOR WRTING ON DB)

            }//END LOOP OVER THE SLICES

        }//END TRY
        catch (Exception e) {
            System.err.println("GENERIC ERROR");
            System.err.println(e.getMessage());
        }

    }

}
