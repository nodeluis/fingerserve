package com.fingerprint.sisrapsur;

import com.digitalpersona.onetouch.*;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.DPFPCapturePriority;
import com.digitalpersona.onetouch.capture.event.DPFPDataEvent;
import com.digitalpersona.onetouch.capture.event.DPFPDataListener;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusEvent;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.readers.DPFPReadersCollection;
import com.digitalpersona.onetouch.verification.DPFPVerification;
import com.digitalpersona.onetouch.verification.DPFPVerificationResult;
import org.glassfish.grizzly.compression.lzma.impl.Base;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Base64;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class FingerPrint extends Thread{

    private String activeReader;
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private FingerServerEndPoint server;

    public void run(){
        switch(server.getState()){

            case FingerServerEndPoint.REGISTER_STATE:
                this.register();
                break;
            case FingerServerEndPoint.VERIFY_STATE:
                this.verify(server.getFingerprintData());
                break;

        }
    }

    FingerPrint(FingerServerEndPoint server){
        this.server = server;
        try {
            this.activeReader = this.getFirstReader();
        }catch (Exception e){
            logger.info(e.toString());
        }

        logger.info("current reader: "+ this.activeReader);
    }

    private void register(){
        if("".equals(this.activeReader)){
            logger.info("No se han encontrado lectores");
            return;
        }
        logger.info("Preparado para registrar en: "+ this.activeReader);
        //Huella pulgar
        //DPFPFingerIndex fingerIndex = DPFPFingerIndex.RIGHT_THUMB;
        try{
            DPFPFeatureExtraction featureExtractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
            DPFPEnrollment enrollment = DPFPGlobal.getEnrollmentFactory().createEnrollment();
            while (enrollment.getFeaturesNeeded()>0){
                DPFPSample sample = this.getSample(this.activeReader);
                if(sample == null)
                    continue;
                DPFPFeatureSet featureSet;
                try{
                    featureSet = featureExtractor.createFeatureSet(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
                }catch (DPFPImageQualityException ex){
                    server.sendResponse(getResponse("bad-image", "Mala calidad en toma de huella."));
                    continue;
                }
                JSONObject response = getResponse("fingerprint-success", "Correcto");
                enrollment.addFeatures(featureSet);
                response.put("remainder", enrollment.getFeaturesNeeded());
                server.sendResponse(response);
            }
            DPFPTemplate template = enrollment.getTemplate();
            JSONObject response = getResponse("register-success", "Huella correcta");
            response.put("template_data", Base64.getEncoder().encodeToString(template.serialize()));
            server.sendResponse(response);
        }catch(DPFPImageQualityException ex){
            logger.info("Excepcion: " + ex.toString());
            server.sendResponse(getResponse("bad-image", "Mala calidad de en toma de huella."));
        }catch(InterruptedException ex){
            throw new RuntimeException(ex);
        }
    }

    private void verify2(String fingerprint){
        logger.info("verify ready");
        JSONObject response = getResponse("verify-response", "Fallo al verificar huella");
        //logger.info("huella "+ fingerprint);
        response.put("success", false);
        if(fingerprint.length()==0 || "".equals(activeReader)){
            server.sendResponse(response);
            return;
        }
        try{
            DPFPSample sample = getSample(activeReader);
            if(sample == null){
                server.sendResponse(response);
                return;
            }

            DPFPFeatureExtraction featureExtractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();

            DPFPFeatureSet featureSet = featureExtractor.createFeatureSet(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);

            DPFPVerification matcher = DPFPGlobal.getVerificationFactory().createVerification();
            matcher.setFARRequested(DPFPVerification.LOW_SECURITY_FAR);

            DPFPTemplate template = getTemplate(fingerprint);
            if(template!=null){
                DPFPVerificationResult result = matcher.verify(featureSet, template);
                if(result.isVerified()){
                    response = getResponse("verify-response", "Validado correctamente");
                    response.put("success", true);
                }
                server.sendResponse(response);
                return;
            }
        }catch(Exception e){
            logger.info("Exception "+ e.toString());
        }
        server.sendResponse(response);
    }

    private void verify(String data){
        JSONObject response = getResponse("verify-response", "Fallo al verificar huella");
        JSONArray arr=new JSONArray(data);
        /*String[] da=data.split(",");
        for (int i = 0; i < da.length; i++) {
            logger.info((new JSONObject(da[i]))+"");
        }*/
        response.put("success", false);
        /*if(fingerprint.length()==0 || "".equals(activeReader)){
            server.sendResponse(response);
            return;
        }*/
        try{
            DPFPSample sample = getSample(activeReader);
            if(sample == null){
                server.sendResponse(response);
                return;
            }

            DPFPFeatureExtraction featureExtractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
            DPFPFeatureSet featureSet = featureExtractor.createFeatureSet(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);
            DPFPVerification matcher = DPFPGlobal.getVerificationFactory().createVerification();
            matcher.setFARRequested(DPFPVerification.LOW_SECURITY_FAR);

            boolean t=false;

            JSONObject resp=new JSONObject();

            for (int i = 0; i < arr.length()&&!t; i++) {
                JSONObject ob=arr.getJSONObject(i);

                DPFPTemplate template1 = getTemplate(ob.getString("pulgar"));
                if(template1!=null){
                    DPFPVerificationResult result = matcher.verify(featureSet, template1);
                    if(result.isVerified()){
                        t=true;
                        resp=ob;
                        break;
                    }
                }
                DPFPTemplate template2 = getTemplate(ob.getString("indice"));
                if(template2!=null){
                    DPFPVerificationResult result = matcher.verify(featureSet, template2);
                    if(result.isVerified()){
                        t=true;
                        resp=ob;
                        break;
                    }
                }
                DPFPTemplate template3 = getTemplate(ob.getString("medio"));
                if(template3!=null){
                    DPFPVerificationResult result = matcher.verify(featureSet, template3);
                    if(result.isVerified()){
                        t=true;
                        resp=ob;
                        break;
                    }
                }
                DPFPTemplate template4 = getTemplate(ob.getString("anular"));
                if(template4!=null){
                    DPFPVerificationResult result = matcher.verify(featureSet, template4);
                    if(result.isVerified()){
                        t=true;
                        resp=ob;
                        break;
                    }
                }
                DPFPTemplate template5 = getTemplate(ob.getString("menique"));
                if(template5!=null){
                    DPFPVerificationResult result = matcher.verify(featureSet, template5);
                    if(result.isVerified()){
                        t=true;
                        resp=ob;
                        break;
                    }
                }
            }
            if(t){
                response = getResponse("verify-response", resp.getString("name"));
                response.put("data",resp.toString());
                response.put("success", t);
            }else{
                response = getResponse("verify-response", "Huella no encontrada");
                response.put("data","");
                response.put("success", t);
            }
        }catch(Exception e){
            logger.info("Exception "+ e.toString());
        }
        server.sendResponse(response);
    }

    private String getFirstReader(){
        DPFPReadersCollection readers = DPFPGlobal.getReadersFactory().getReaders();
        if(readers == null || readers.size() == 0){
            server.sendResponse(this.getResponse("reader-not-found","No se han encontrado lectores"));
            return "";
        }
        return readers.get(0).getSerialNumber();
    }

    private JSONObject getResponse(String type, String message){
        JSONObject response = new JSONObject();
        response.put("type", type);
        response.put("message", message);
        return response;
    }

    private DPFPSample getSample(String activeReader) throws InterruptedException{
        final LinkedBlockingQueue<DPFPSample> samples = new LinkedBlockingQueue<DPFPSample>();
        DPFPCapture capture = DPFPGlobal.getCaptureFactory().createCapture();
        capture.setReaderSerialNumber(activeReader);
        capture.setPriority(DPFPCapturePriority.CAPTURE_PRIORITY_LOW);
        capture.addDataListener(new DPFPDataListener() {
            @Override
            public void dataAcquired(DPFPDataEvent e) {
                if(e!=null && e.getSample() != null){
                    try{
                        samples.put(e.getSample());
                    }catch (InterruptedException ex){
                        ex.printStackTrace();
                    }
                }
            }
        });
        capture.addReaderStatusListener(new DPFPReaderStatusAdapter(){
            int lastStatus = DPFPReaderStatusEvent.READER_CONNECTED;
            public void readerConnected(DPFPReaderStatusEvent e){
                if(lastStatus!=e.getReaderStatus()){
                    logger.info("Reader is connected");
                }
                lastStatus = e.getReaderStatus();
            }
            public void readerDisconnected(DPFPReaderStatusEvent e){
                if(lastStatus != e.getReaderStatus()){
                    logger.info("Reader is disconnected");
                }
                lastStatus = e.getReaderStatus();
            }
        });
        try{
            capture.startCapture();
            return samples.take();
        }catch (RuntimeException e){
            logger.info("Exception " + e.toString());
            throw e;
        }finally {
            capture.stopCapture();
        }
    }

    DPFPTemplate getTemplate(String fingerprint){
        byte[] fingerPrintBytes = Base64.getDecoder().decode(fingerprint);
        DPFPTemplate template = DPFPGlobal.getTemplateFactory().createTemplate(fingerPrintBytes);
        return template;
    }

}
