package it.aslcn1.ssde;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.Opener;
import ij.measure.Measurements;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageStatistics;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class Utils {
    public static final int sogliaHU = -410;

    public static String ExtractInfoFromTag(String HEADER, String TAG) {
        int STlen = 15; //gggg,eeee + " ---:"
        String INFO = "";
        // System.err.println("TAG: "+TAG);
        if (HEADER != null) {
            int index1 = HEADER.indexOf(TAG);

            int index2 = HEADER.indexOf(":", index1);
            int index3 = HEADER.indexOf("\n", index2);
            if (index1 >= 0 && index2 >= 0 && index3 >= 0) {
                INFO = HEADER.substring(index1 + STlen, index3);
                INFO = INFO.trim();
            }
        }
        return INFO;
    }

    public static String ExtractInfo(String HEADER, String ST1, String ST2) {
        int STlen = ST1.length();
        String INFO = "";
        if (HEADER != null) {
            int index1 = HEADER.indexOf(ST1);
            int index2 = HEADER.indexOf(ST2, index1);
            if (index1 >= 0 && index2 >= 0) {
                INFO = HEADER.substring(index1 + STlen, index2);
                INFO = INFO.trim();
            }
        }
        return INFO;
    }

    public static float getPixelDimensionFromHeader(ImagePlus imp) {
        float dimPx = 0.0f;

        String header = (String) imp.getInfoProperty();

        String HeadDimPix = ExtractInfo(header, "Pixel Spacing:", "\\");
        if (HeadDimPix == "") {
            //  dimPx=0.195f;}//eurocolumbus raw (1536x1536)
            dimPx = Float.NaN;
        }//eurocolumbus raw (1536x1536)
        else {
            dimPx = (float) (Double.parseDouble(HeadDimPix));
        }
        return dimPx;
    }

    public static Roi getCentralRoi(List<Roi> elencoRoi, int righe, int colonne){
        Roi result = null;
        HashMap<Double, Roi> mapRois = new HashMap();
        for(Roi r:elencoRoi){
            double[] centroid = r.getContourCentroid();
            //LE COORD SONO IN PIXEL
            double x_centro = 0.5*colonne;
            double y_centro = 0.5*righe;
            double distanza_dal_centro = Math.sqrt((centroid[0]-x_centro)*(centroid[0]-x_centro)+(centroid[1]-y_centro)*(centroid[1]-y_centro));
//            System.err.println(distanza_dal_centro);
            mapRois.put(distanza_dal_centro,r);

        }
        //CON LA MAPPA CERCO IL MINIMO
        Double min = Collections.min(mapRois.keySet());
        result = mapRois.get(min);
        return result;
    }

    public static int[] getTruncationFromCTImage(ImagePlus imp, Rectangle bounds, Roi body){
        //imp.show();
        int[] results = new int[8]; //0 e 1 sin 2 e 3 dx 4 e 5 top 6 e 7 bottom
        //inizializzo a -1 // se resta -1 non era troncata
        for(int i = 0;i<results.length;i++)
        {
            results[i] = -1;
        }


        int x_upper_left = bounds.x;
        int y_upper_left = bounds.y;
        int x_lower_right = x_upper_left + bounds.width-1;
        int y_lower_right = y_upper_left+bounds.height-1;

        ArrayList<Integer> listaCoordinateY = new ArrayList();
        ArrayList<Integer> listaCoordinateX = new ArrayList();

        boolean sottoSoglia = true;
        boolean listaDispari = false;
        //lato sinistro


        double firstPV = 0;
        if(x_upper_left==0)
        {
            //SINISTRO
            firstPV =imp.getProcessor().getPixelValue(x_upper_left, y_upper_left );
            for(int i=0;i<bounds.height-1;i++) // -1 per arrivare a consid l'ultimo pixel succ
            {

                sottoSoglia = firstPV < sogliaHU; //se primo pv sotto soglia
                listaDispari = !((listaCoordinateY.size() & 1) == 0);
                //IJ.log("LISTA DISPARI? "+listaDispari);
                double nextPV = imp.getProcessor().getPixelValue(x_upper_left,y_upper_left+(i+1));

                if (!(sottoSoglia ^ listaDispari)){
                    if((nextPV < sogliaHU) ){
                        listaCoordinateY.add(y_upper_left+i);
                    }

                }
                else
                {
                    if((nextPV > sogliaHU) && (body.contains(x_upper_left,y_upper_left+(i+1)))){
                        listaCoordinateY.add(y_upper_left+i+1);
                    }
                }

            }//fine giro for su bordo sinistro


            //imp.show();
            //PRENDO PRIMO E ULTIMO
            if(listaCoordinateY.size()>0) {
                int y1 = listaCoordinateY.get(0);
                int y2 = listaCoordinateY.size()==1?y_lower_right:listaCoordinateY.get(listaCoordinateY.size()-1);
                results[0] = y1;
                results[1] = y2;

            }

            listaCoordinateY.clear();
        }
        if(x_lower_right==(imp.getWidth()-1))
        {
            firstPV =imp.getProcessor().getPixelValue(x_lower_right, y_upper_left );

            for(int i=0;i<bounds.height-1;i++) // -1 per arrivare a consid l'ultimo pixel succ
            {

                sottoSoglia = firstPV < sogliaHU; //se primo pv sotto soglia
                listaDispari = !((listaCoordinateY.size() & 1) == 0);
                //IJ.log("LISTA DISPARI? "+listaDispari);
                double nextPV = imp.getProcessor().getPixelValue(x_lower_right,y_upper_left+(i+1));

                if (!(sottoSoglia ^ listaDispari)){
                    if((nextPV < sogliaHU) ){
                        listaCoordinateY.add(y_upper_left+i);
                    }

                }
                else
                {
                    if((nextPV > sogliaHU) && (body.contains(x_lower_right,y_upper_left+(i+1)))){
                        listaCoordinateY.add(y_upper_left+i+1);
                    }
                }

            }//fine giro for su bordo DESTRO


            //imp.show();
            //PRENDO PRIMO E ULTIMO
            if(listaCoordinateY.size()>0) {
                int y1 = listaCoordinateY.get(0);
                int y2 = listaCoordinateY.size()==1?y_lower_right:listaCoordinateY.get(listaCoordinateY.size()-1);
                results[2] = y1;
                results[3] = y2;

            }
            listaCoordinateY.clear();



        }//FINE SE DESTRO

        if(y_upper_left==0)
        {
            //TOP

            firstPV =imp.getProcessor().getPixelValue(x_upper_left, y_upper_left );
            for(int i=0;i<bounds.width-1;i++) // -1 per arrivare a consid l'ultimo pixel succ
            {

                sottoSoglia = firstPV <sogliaHU; //se primo pv sotto soglia
                listaDispari = !((listaCoordinateX.size() & 1) == 0);
                //IJ.log("LISTA DISPARI? "+listaDispari);
                double nextPV = imp.getProcessor().getPixelValue(x_upper_left+i,y_upper_left);
                boolean addToCoord = false;
                if (!(sottoSoglia ^ listaDispari)){
                    if((nextPV < sogliaHU) ){
                        listaCoordinateX.add(i);
                    }

                }
                else
                {
                    if((nextPV > sogliaHU) && (body.contains(x_upper_left+i+1,y_upper_left))){
                        listaCoordinateX.add(i+1);
                    }
                }

            }//fine giro for su bordo TOP


            //imp.show();
            //PRENDO PRIMO E ULTIMO
            if(listaCoordinateX.size()>0) {
                int x1 = listaCoordinateX.get(0);
                int x2 = listaCoordinateX.size()==1?x_lower_right:listaCoordinateX.get(listaCoordinateX.size()-1);
                results[4]= x1;
                results[5]=x2;
            }

            listaCoordinateX.clear();
        }//FINE SU TOP
        if(y_lower_right==(imp.getHeight()-1)){
            //BOTTOM


            firstPV =imp.getProcessor().getPixelValue(x_upper_left, y_lower_right );
            for(int i=0;i<bounds.width-1;i++) // -1 per arrivare a consid l'ultimo pixel succ
            {

                sottoSoglia = firstPV < sogliaHU; //se primo pv sotto soglia
                listaDispari = !((listaCoordinateX.size() & 1) == 0);
                //IJ.log("LISTA DISPARI? "+listaDispari);
                double nextPV = imp.getProcessor().getPixelValue(x_upper_left+i,y_lower_right);

                if (!(sottoSoglia ^ listaDispari)){
                    if((nextPV < sogliaHU) ){
                        listaCoordinateX.add(i);
                    }

                }
                else
                {
                    if((nextPV > sogliaHU) && (body.contains(x_upper_left+i+1,y_lower_right))){
                        listaCoordinateX.add(i+1);
                    }
                }

            }//fine giro for su bordo TOP


            //imp.show();
            //PRENDO PRIMO E ULTIMO
            if(listaCoordinateX.size()>0) {
                int x1 = listaCoordinateX.get(0);
                int x2 = listaCoordinateX.size()==1?x_lower_right:listaCoordinateX.get(listaCoordinateX.size()-1);
                results[6] = x1;
                results[7] = x2;

            }

            listaCoordinateX.clear();

        }//FINE BOTTOM

        return results;
    }

    public static String getProtocolFromCTImage(final ImagePlus imp){
        String result = "OTHER";
        String protocolFromHeader = ExtractInfo(imp.getInfoProperty(), "Protocol Name:", "\n");
        String descrizioneEsame = ExtractInfo(imp.getInfoProperty(), "Study Description:", "\n");
        if(!protocolFromHeader.isEmpty())
        {
            if  (protocolFromHeader.toUpperCase().contains("TESTA") || protocolFromHeader.toUpperCase().contains("HEAD") || protocolFromHeader.toUpperCase().contains("NECK")
                    || protocolFromHeader.toUpperCase().contains("COLLO") || protocolFromHeader.toUpperCase().contains("CRANIO") || protocolFromHeader.toUpperCase().contains("ENCEFALO")
                    || protocolFromHeader.toUpperCase().contains("BRAIN")){
                result ="HEAD";
            }
            else if(protocolFromHeader.toUpperCase().contains("ABDOMEN") ||protocolFromHeader.toUpperCase().contains("ADDOME")
                    ||protocolFromHeader.toUpperCase().contains("PELVIS")||protocolFromHeader.toUpperCase().contains("PELVI")   ){
                //SE ESAME DICE TORACE VALE TORACE
                if(descrizioneEsame.toUpperCase().contains("THORAX") || descrizioneEsame.toUpperCase().contains("TORACE")){
                    result = "THORAX";
                }else{result = "ABDOMEN";}
            }
            else if(protocolFromHeader.toUpperCase().contains("TORACE") || protocolFromHeader.toUpperCase().contains("THORAX")
            ){
                result = "THORAX";
            }
        }
        return result;
    }

    public static double[] getWaterEquivalentInfoFromCTImage(ImagePlus imp){
        double[] results = new double[5]; // 0: Dw 1: Area eq e 2 è la perce3ntuale di tronc; 3 e 4 coord x e y CENTROIDE
        results[0] = Double.NaN;
        results[1] = Double.NaN;
        results[2] = 0d; //inizializzo con 0 (opotesi non troncata)
        results[3] = Double.NaN;
        results[4] = Double.NaN;

        int righe = Integer.parseInt(ExtractInfo(imp.getInfoProperty(),"Rows:","\n"));
        int colonne = Integer.parseInt(ExtractInfo(imp.getInfoProperty(),"Columns:","\n"));
        double dimPx = getPixelDimensionFromHeader(imp);

        // System.err.println("ARRAY A NAN");
        IJ.setThreshold(imp, -410, 32767);
        //System.err.println("SOGLIA IMPOSTATA");
        RoiManager rm = RoiManager.getInstance2();

        if (rm == null) {
            rm = new RoiManager(false);
        }
        if(rm==null){
            //  System.err.println("NO ROI MANAGER!");
        }
        // System.err.println("ROI MANAGER CREATO");
        ParticleAnalyzer.setRoiManager(rm);
        // System.err.println("ROI MANAGER IMPOSTATO"); //DA RIVEDERE 5000 per roi piccole /gambe braccia polso ecc)
        IJ.run(imp, "Analyze Particles...", "size=3000-Infinity clear add ");
        // System.err.println("ANALISI PARTICELLE FATTA");

        Roi[] rois = rm.getRoisAsArray();

        if(rois==null || rois.length==0) {
            //System.err.println("INVERTO?");
            IJ.run(imp, "Invert LUT", "");
            IJ.setThreshold(imp, -410, 32767);
            IJ.run(imp, "Analyze Particles...", "size=3000-Infinity clear add");
            rois = rm.getRoisAsArray();

        }
        // System.err.println("TROVATO IL BODY?");
        //  System.err.println("rois.length: "+rois.length);
        Roi roiBody = null;
        if(rois == null || rois.length != 1)
        {
            //TEST SU MULTIPLE ROI
            Roi centrale = getCentralRoi(Arrays.asList(rois),righe,colonne);
            roiBody = centrale;
            //rm.close();
            //System.err.println("BODY NON TROVATO O PIU DI UNA ROI");
            //return results; // ERRORE TROVATO PIU DI UN BODY O NON TROVATO TORNANO NaN!!
        }
        else{
            roiBody = rois[0];
        }
        //System.err.println("RESET");
        rm.reset();
        //System.err.println("CLOSE");
        rm.close();
        rm = null;
        // System.err.println("MESSO A NULL");


        double[] centroid =roiBody.getContourCentroid();

        double x = (centroid[0]-colonne/2)*dimPx;
        double y = (centroid[1]-righe/2)*dimPx;
        results[3] = x;
        results[4] = y;
        imp.setRoi(roiBody);
        //è troncata?
        Rectangle bounds =  roiBody.getBounds();
        int x_upper_left = bounds.x;
        int y_upper_left = bounds.y;
        int x_lower_right = x_upper_left + bounds.width-1;
        int y_lower_right = y_upper_left+bounds.height-1;
        int imageHeight = imp.getHeight();
        int imageWidth = imp.getWidth();
        boolean truncated = false;
        if((x_upper_left==0 || y_upper_left==0) || (x_lower_right+1==imageWidth || y_lower_right+1 ==imageHeight))
        {
            truncated = true;
        }

        if(truncated){
            imp.killRoi();
            imp.updateAndDraw();
            imp.updateAndRepaintWindow();

            String header = (String) imp.getProperty("Info");
            String sRescaleIntercept = ExtractInfo(header, "Rescale Intercept:", "\n");
            int intercept = Integer.parseInt(sRescaleIntercept);
            double percentage=0d;
            int coordsTruncation[] =getTruncationFromCTImage(imp,bounds,roiBody);
            if(coordsTruncation[0]>=0)
            {
                //TRONCATA A SINISTRA
                for(int i=0;coordsTruncation[0]+i<=coordsTruncation[1];i++)
                {
                    imp.getProcessor().putPixelValue(x_upper_left,coordsTruncation[0]+i,-intercept);
                }
                imp.updateAndRepaintWindow();
                percentage += Math.abs(coordsTruncation[0]-coordsTruncation[1])*dimPx;
            }
            if(coordsTruncation[2]>=0)
            {
                //troncata a DESTRA

                for(int i=0;coordsTruncation[2]+i<=coordsTruncation[3];i++)
                {
                    imp.getProcessor().putPixelValue(x_lower_right,coordsTruncation[2]+i,-intercept);
                }
                imp.updateAndRepaintWindow();
                percentage += Math.abs(coordsTruncation[2]-coordsTruncation[3])*dimPx;

            }
            if(coordsTruncation[4]>=0)
            {
                //TRONCATA TOP
                for(int i=0;coordsTruncation[4]+i<=coordsTruncation[5];i++)
                {
                    imp.getProcessor().putPixelValue(coordsTruncation[4]+i,y_upper_left,-intercept);
                }
                imp.updateAndRepaintWindow();
                percentage += Math.abs(coordsTruncation[4]-coordsTruncation[5])*dimPx;
            }
            if(coordsTruncation[6]>=0)
            {
                //TRONCATA BOTTOM
                for(int i=0;coordsTruncation[6]+i<=coordsTruncation[7];i++)
                {
                    imp.getProcessor().putPixelValue(coordsTruncation[6]+i,y_lower_right,-intercept);
                }
                imp.updateAndRepaintWindow();
                percentage += Math.abs(coordsTruncation[6]-coordsTruncation[7])*dimPx;
            }

            rm=null;
            rm = new RoiManager(false);

            if (rm == null) {
                rm = new RoiManager(false);
            }
            ParticleAnalyzer.setRoiManager(rm);
            imp.unlock();
            IJ.resetThreshold(imp);

            IJ.setThreshold(imp, -410, 32000);
            rm.select(0);
            // System.err.println("ROI MANAGER IMPOSTATO");
            IJ.run(imp, "Analyze Particles...", "size=3000-Infinity clear add ");
            // System.err.println("ANALISI PARTICELLE FATTA");

            rois = rm.getRoisAsArray();
            if(rois == null || rois.length != 1)
            {
                //TEST SU MULTIPLE ROI
                Roi centrale = getCentralRoi(Arrays.asList(rois),righe,colonne);
                roiBody = centrale;
                //rm.close();
                //System.err.println("BODY NON TROVATO O PIU DI UNA ROI");
                //return results; // ERRORE TROVATO PIU DI UN BODY O NON TROVATO TORNANO NaN!!
            }
            else{
                roiBody = rois[0];
            }
            //System.err.println("RESET");
            rm.reset();
            //System.err.println("CLOSE");
            rm.close();
            rm = null;
            // System.err.println("MESSO A NULL");

            roiBody = rois[0];
            imp.setRoi(roiBody);
            imp.updateAndRepaintWindow();

            percentage /= 0.01*roiBody.getLength();
            results[2] = percentage;
        }//FINE SE TRONCATA : LA ROI è STATA "AGGIUSTATA" PER PRENDERE IN TOTO IL BODY E NON SEGUIRE I POLMONI VERSO L'INTERNO

        double kAnam =1.0; //The size-specific dose estimate (SSDE) for truncated computed tomography images
        String protocol = getProtocolFromCTImage(imp);
        if(truncated){
            double TP = results[2];

            if(protocol.equals("ABDOMEN")||protocol.equals("OTHER"))
            {
                kAnam = Math.exp( 1.14E-06*Math.pow(TP,3));
            }
            else if(protocol.equals("THORAX")){
                kAnam = Math.exp( 1.15E-06*Math.pow(TP,3));
            }else if(protocol.equals("HEAD")){
                kAnam = Math.exp( 0.85E-06*Math.pow(TP,3));
            }
//            System.err.println("k Anam for correcting truncation is: "+IJ.d2s(kAnam,2));
        }


        // System.err.println("ROI SETTATA");
        ImageStatistics warStat = imp.getStatistics();
        ImageStatistics stat = ImageStatistics.getStatistics(imp.getProcessor(), Measurements.MEAN,imp.getCalibration());
        // System.err.println("STAT PRESE");
        double HU = stat.mean;
        double areaMiddle =stat.area; results[1] = areaMiddle;
        double AwMiddle = areaMiddle*(0.001*HU+1);
        double DwMiddle = 0.2*Math.sqrt(AwMiddle/Math.PI)*kAnam;
        results[0] = DwMiddle;

        return results;
    }

    public static void sortDirectoryByImageNumber(String dir){
        File workingDir = new File(dir);
        File[] listFile = workingDir.listFiles();

        int count=1;
        try{
            Files.createDirectories(Paths.get(dir+"/sorted"));
        }
        catch(IOException e){
            System.err.println(e.getMessage());
        }

        for (File f : listFile) {
            if (f.isDirectory())
            {
                continue;
            }
            //System.err.println("APRO IL FILE");
            ImagePlus img = new Opener().openImage(f.getPath());

            String header = (String) img.getProperty("Info");

            //chiudo le ImagePlus
            img.changes = false; //evito la richiesta di salvare prima di chiudere

            img.close();

            //se manca l'header ritorna 0 (non fa nessun compare)
            if (header == null) {
                IJ.error("error! Found a file with no DICOM header.");
                return;


            }

            String imageNumber = ExtractInfo(header, "Image Number:", "\n");
            if(imageNumber.isEmpty())
            {

                continue; // non è un file con il tag slice Location --skip
            }
            Path path = Paths.get(f.getAbsolutePath());


            try {
                Path FROM = path;
                Path TO = Paths.get(dir,"sorted",imageNumber);
                // System.err.println("TO: "+TO);
                //path.resolveSibling("/sorted/"+sliceLocation + ".dcm");
                //overwrite the destination file if it exists, and copy
                // the file attributes, including the rwx permissions
                CopyOption[] options = new CopyOption[]{

                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES
                };

                // Files.move(path, path.resolveSibling(sliceLocation + ".dcm"));

                Files.copy(FROM, TO, options);
            } catch (IOException ex) {

            }

        }
    }

}
