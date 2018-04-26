package service;

import java.util.*;
import components.data.*;
import business.*;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import javax.xml.bind.*;

public class LAMSAppointmentService {

   private DBSingleton dbSingleton;

   public String initialize() {
      dbSingleton = DBSingleton.getInstance();
      dbSingleton.db.initialLoad("LAMS");
      return "Database Initialized";
   }
   
   public String getAllAppointments() throws Exception {
      String result = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?> <AppointmentList>";
      List<Object> objs;
      
      dbSingleton = DBSingleton.getInstance();
      objs = dbSingleton.db.getData("Appointment", "");
      
      if (objs == null || objs.isEmpty()) {
         dbSingleton.db.initialLoad("LAMS");
         objs = dbSingleton.db.getData("Appointment", "");
      }
      
      if (objs!= null) {
         JAXBContext jaxbContext = JAXBContext.newInstance(Appointment.class);
   		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
         DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
         
         dbf.setNamespaceAware(true);
         DocumentBuilder db = dbf.newDocumentBuilder();
         
         for (Object obj : objs) {
            Document doc = db.newDocument();
            jaxbMarshaller.marshal(obj, doc);
         
            result += doc.toString();
         }
      }
      
      return result + "</AppointmentList> ";
   }
   
   public String getAppointment(String appointmentNumber) {
      String result = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?> <AppointmentList>";
      List<Object> objs;
      
      dbSingleton = DBSingleton.getInstance();
      objs = dbSingleton.db.getData("Appointment", "id='" + appointmentNumber + "'");
      
      if (objs == null || objs.isEmpty()) {
         dbSingleton.db.initialLoad("LAMS");
         objs = dbSingleton.db.getData("Appointment", "id='" + appointmentNumber + "'");
      }
      
      Patient patient = null;
      Phlebotomist phleb = null;
      PSC psc = null;
      
      if (objs != null) {
         for (Object obj : objs){
            result += obj;
         }
      }
      
      if (objs == null || objs.isEmpty()) {
         result = "<error>ERROR:Appointment is not available</error>";
      }
      
      return result + "</AppointmentList>";
   }
   
   public String addAppointment(String xmlStyle) throws Exception {
      String result = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?> <AppointmentList>";
      String aptId = createAppointmentId();
      
      Document doc = parseXmlFromString(xmlStyle);
      doc.getDocumentElement().normalize();
      
      Node node = doc.getElementsByTagName("appointment").item(0);
      Element appointment = (Element) node;
      
      String date = appointment.getElementsByTagName("date").item(0).getTextContent();
      String time = appointment.getElementsByTagName("time").item(0).getTextContent();
      String physicianId = appointment.getElementsByTagName("physicianId").item(0).getTextContent();
      String pscId = appointment.getElementsByTagName("pscId").item(0).getTextContent();
      String patientId = appointment.getElementsByTagName("patientId").item(0).getTextContent();
      String phlebotomistId = appointment.getElementsByTagName("phlebotomistId").item(0).getTextContent();
      
      Patient patient = getPatient(patientId);
      Phlebotomist phlebotomist = getPhlebotomist(phlebotomistId);
      PSC psc = getPSC(pscId);
      
      List<AppointmentLabTest> labTests = new ArrayList<AppointmentLabTest>();
      NodeList labTestsNL = appointment.getElementsByTagName("labTests");
      for (int i = 0; i < labTestsNL.getLength(); i++) {
         Node testNode = doc.getElementsByTagName("test").item(0);
         Element labTestElement = (Element)testNode;
         AppointmentLabTest labTest = new AppointmentLabTest(aptId,labTestElement.getAttribute("id"),labTestElement.getAttribute("dxcode"));
         
         labTest.setDiagnosis((Diagnosis)dbSingleton.db.getData("Diagnosis", "code='" + labTestElement.getAttribute("dxcode") + "'").get(0));
         labTest.setLabTest((LabTest)dbSingleton.db.getData("LabTest","id='" + labTestElement.getAttribute("id") + "'").get(0));
         
         labTests.add(labTest);
      }
      
      Appointment newAppt = new Appointment(aptId,java.sql.Date.valueOf(date),java.sql.Time.valueOf(time));
      newAppt.setAppointmentLabTestCollection(labTests);
      newAppt.setPatientid(patient);
      newAppt.setPhlebid(phlebotomist);
      newAppt.setPscid(psc);
      
      boolean good = dbSingleton.db.addData(newAppt);
      List<Object> objs = dbSingleton.db.getData("Appointment", "patientid='" + aptId + "'");
      
      if (objs != null) {
         for (Object obj : objs){
            result += obj;
         }
      }

      return result + "</appointment>";
   }
   
   private Document parseXmlFromString(String xmlString) throws Exception {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      InputStream inputStream = new    ByteArrayInputStream(xmlString.getBytes());
      org.w3c.dom.Document document = builder.parse(inputStream);
      return document;
   }
   
   private String createAppointmentId() throws Exception {
      String xmlAppointments = getAllAppointments();
      
      Document aptDoc = parseXmlFromString(xmlAppointments);
      aptDoc.getDocumentElement().normalize();
      
      NodeList nodeList = aptDoc.getElementsByTagName("AppointmentList");
      
      int highestId = -1;
      
      for (int i = 0; i < nodeList.getLength(); i++) {
         Element appointment = (Element) nodeList.item(i);
         
         int aptId = Integer.valueOf(appointment.getAttribute("id"));
         highestId = aptId > highestId ? aptId : highestId;
      }
      
      return String.valueOf(highestId++);
   }
   
   private Patient getPatient(String patientId) {
      return (Patient) dbSingleton.db.getData("Patient", "id='" + patientId + "'").get(0);
   }
   
   private Phlebotomist getPhlebotomist(String phlebotomistId) {
      return (Phlebotomist) dbSingleton.db.getData("Phlebotomist", "id='" + phlebotomistId + "'").get(0);
   }
   
   private PSC getPSC(String PSCId) {
      return (PSC) dbSingleton.db.getData("PSC", "id='" + PSCId + "'").get(0);
   }
}