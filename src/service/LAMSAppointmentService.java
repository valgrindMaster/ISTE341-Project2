package service;

import java.util.*;
import components.data.*;
import business.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import javax.xml.transform.*;
import javax.xml.bind.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jws.WebService;
import javax.jws.WebMethod;

@WebService(serviceName = "LAMSAppointmentService")
public class LAMSAppointmentService {

   private DBSingleton dbSingleton;
   
   public LAMSAppointmentService() {
      initialize();
   }

   @WebMethod(operationName = "initialize")
   public String initialize() {
      dbSingleton = DBSingleton.getInstance();
      dbSingleton.db.initialLoad("LAMS");
      return "Database Initialized";
   }
   
   @WebMethod(operationName = "getAllAppointments")
   public String getAllAppointments() throws Exception {
      List<Object> objs;
      
      dbSingleton = DBSingleton.getInstance();
      objs = dbSingleton.db.getData("Appointment", "");
      
      if (objs == null || objs.isEmpty()) {
         initialize();
         objs = dbSingleton.db.getData("Appointment", "");
      }
      
      String result = "";
      if (objs!= null) {
         DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		   DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
         TransformerFactory tf = TransformerFactory.newInstance();
         Transformer transformer = tf.newTransformer();
         StringWriter writer = new StringWriter();

		   Document doc = docBuilder.newDocument();
         Element rootElement = doc.createElement("AppointmentList");
         doc.appendChild(rootElement);
         
         for (Object obj : objs) {
            Appointment appointment = (Appointment) obj;
            Element aptElement = doc.createElement("appointment");
            
            //Add attributes to appointment tag.
            aptElement = addAptAttr(appointment, aptElement, doc);
            
            //Add patient data.
            aptElement = addPatientsToApt(appointment, aptElement, doc);
            
            //Add phlebotomist data.
            aptElement = addPhlebotomistToApt(appointment, aptElement, doc);
            
            //Add PSC data.
            aptElement = addPSCToApt(appointment, aptElement, doc);
            
            //Add Lab Test data.
            aptElement = addLabTestsToApt(appointment, aptElement, doc);
            
            rootElement.appendChild(aptElement);
         }
         
         transformer.transform(new DOMSource(doc), new StreamResult(writer));
         result += writer.toString();
      }
      
      return result;
   }
   
   @WebMethod(operationName = "getAppointment")
   public String getAppointment(String appointmentNumber) throws Exception {
      List<Object> objs;
      String arg = "id='" + appointmentNumber + "'";
      
      dbSingleton = DBSingleton.getInstance();
      objs = dbSingleton.db.getData("Appointment", arg);
      
      if (objs == null || objs.isEmpty()) {
         initialize();
         objs = dbSingleton.db.getData("Appointment", arg);
      }
      
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	   DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      StringWriter writer = new StringWriter();

	   Document doc = docBuilder.newDocument();
      Element rootElement = doc.createElement("AppointmentList");
      doc.appendChild(rootElement);
      
      String result = "";
      if (objs != null && objs.isEmpty()) {
         Element errElement = doc.createElement("error");
         errElement.appendChild(doc.createTextNode("ERROR: No Appointment found with matching id"));
		   rootElement.appendChild(errElement);
      } else if (objs != null) {
         Appointment appointment = (Appointment) objs.get(0);
         Element aptElement = doc.createElement("appointment");
         
         //Add attributes to appointment tag.
         aptElement = addAptAttr(appointment, aptElement, doc);
         
         //Add patient data.
         aptElement = addPatientsToApt(appointment, aptElement, doc);
         
         //Add phlebotomist data.
         aptElement = addPhlebotomistToApt(appointment, aptElement, doc);
         
         //Add PSC data.
         aptElement = addPSCToApt(appointment, aptElement, doc);
         
         //Add Lab Test data.
         aptElement = addLabTestsToApt(appointment, aptElement, doc);
         
         rootElement.appendChild(aptElement);
      }
      
      transformer.transform(new DOMSource(doc), new StreamResult(writer));
      result += writer.toString();
      
      return result;
   }
   
   @WebMethod(operationName = "addAppointment")
   public String addAppointment(String xmlStyle) throws Exception {
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
         Element labTestElement = (Element) testNode;
         AppointmentLabTest labTest = new AppointmentLabTest(aptId,labTestElement.getAttribute("id"),labTestElement.getAttribute("dxcode"));
         
         labTest.setDiagnosis((Diagnosis)dbSingleton.db.getData("Diagnosis", "code='" + labTestElement.getAttribute("dxcode") + "'").get(0));
         labTest.setLabTest((LabTest)dbSingleton.db.getData("LabTest", "id='" + labTestElement.getAttribute("id") + "'").get(0));
         
         labTests.add(labTest);
      }
      
      Appointment newAppt = new Appointment(aptId,java.sql.Date.valueOf(date),java.sql.Time.valueOf(time));
      newAppt.setAppointmentLabTestCollection(labTests);
      newAppt.setPatientid(patient);
      newAppt.setPhlebid(phlebotomist);
      newAppt.setPscid(psc);
      
      dbSingleton.db.addData(newAppt);
      return getAppointment(aptId);
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
      
      NodeList nodeList = aptDoc.getElementsByTagName("appointment");
      
      int highestId = -1;
      
      for (int i = 0; i < nodeList.getLength(); i++) {
         Element appointment = (Element) nodeList.item(i);
         
         int aptId = Integer.valueOf(appointment.getAttribute("id"));
         highestId = aptId > highestId ? aptId : highestId;
      }
      
      return String.valueOf(++highestId);
   }
   
   private Element addAptAttr(Appointment appointment, Element aptElement, Document doc) {
      DateFormat dateToStrFormatter = new SimpleDateFormat("yyyy-mm-dd");
      String strDate = dateToStrFormatter.format(appointment.getApptdate());
      
      DateFormat timeToStrFormatter = new SimpleDateFormat("hh:mm:ss");
      String strTime = timeToStrFormatter.format(appointment.getAppttime());
      
      Attr dateAttr = doc.createAttribute("date");
	   dateAttr.setValue(strDate);
	   aptElement.setAttributeNode(dateAttr);
      
      Attr idAttr = doc.createAttribute("id");
	   idAttr.setValue(appointment.getId());
	   aptElement.setAttributeNode(idAttr);
      
      Attr timeAttr = doc.createAttribute("time");
	   timeAttr.setValue(strTime);
	   aptElement.setAttributeNode(timeAttr);
      
      return aptElement;
   }
   
   private Element addPatientsToApt(Appointment appointment, Element aptElement, Document doc) {
      Element patientElement = doc.createElement("patient");
      
      Patient patient = appointment.getPatientid();
      
      DateFormat dateToStrFormatter = new SimpleDateFormat("yyyy-mm-dd");
      String patientDob = dateToStrFormatter.format(patient.getDateofbirth());
      
      Attr idAttr = doc.createAttribute("id");
	   idAttr.setValue(patient.getId());
	   patientElement.setAttributeNode(idAttr);
      
      Element patientNameElement = doc.createElement("name");
      patientNameElement.appendChild(doc.createTextNode(patient.getName()));
		patientElement.appendChild(patientNameElement);

      Element patientAddrElement = doc.createElement("address");
      patientAddrElement.appendChild(doc.createTextNode(patient.getAddress()));
		patientElement.appendChild(patientAddrElement);

      Element patientInsrElement = doc.createElement("insurance");
      patientInsrElement.appendChild(doc.createTextNode(String.valueOf(patient.getInsurance())));
		patientElement.appendChild(patientInsrElement);

      Element patientDobElement = doc.createElement("dob");
      patientDobElement.appendChild(doc.createTextNode(patientDob));
		patientElement.appendChild(patientDobElement);
      
      aptElement.appendChild(patientElement);
      
      return aptElement;
   }
   
   private Element addPhlebotomistToApt(Appointment appointment, Element aptElement, Document doc) {
      Element phlebElement = doc.createElement("phlebotomist");
      
      Phlebotomist phlebotomist = appointment.getPhlebid();
      
      Attr idAttr = doc.createAttribute("id");
	   idAttr.setValue(phlebotomist.getId());
	   phlebElement.setAttributeNode(idAttr);
      
      Element phlebNameElement = doc.createElement("name");
      phlebNameElement.appendChild(doc.createTextNode(phlebotomist.getName()));
		phlebElement.appendChild(phlebNameElement);
      
      aptElement.appendChild(phlebElement);
      
      return aptElement;
   }
   
   private Element addPSCToApt(Appointment appointment, Element aptElement, Document doc) {
      Element pscElement = doc.createElement("psc");
      
      PSC psc = appointment.getPscid();
      
      Attr idAttr = doc.createAttribute("id");
	   idAttr.setValue(psc.getId());
	   pscElement.setAttributeNode(idAttr);
      
      Element pscNameElement = doc.createElement("name");
      pscNameElement.appendChild(doc.createTextNode(psc.getName()));
		pscElement.appendChild(pscNameElement);
      
      aptElement.appendChild(pscElement);
      
      return aptElement;
   }
   
   private Element addLabTestsToApt(Appointment appointment, Element aptElement, Document doc) {
      Element allLabTestsElement = doc.createElement("allLabTests");
      
      List<AppointmentLabTest> labTests = appointment.getAppointmentLabTestCollection();
      
      for (int i = 0; i < labTests.size(); i++) {
         AppointmentLabTest labTest = labTests.get(i);
         
         System.out.println(labTest.getAppointment());
         
         Element labTestElement = doc.createElement("appointmentLabTest");
      
         if (labTest.getAppointment() != null) {
            Attr aptIdAttr = doc.createAttribute("appointmentId");
      	   aptIdAttr.setValue(labTest.getAppointment().getId());
      	   labTestElement.setAttributeNode(aptIdAttr);
         }
         
         if (labTest.getDiagnosis() != null) {
            Attr dxcodeAttr = doc.createAttribute("dxcode");
      	   dxcodeAttr.setValue(labTest.getDiagnosis().getCode());
      	   labTestElement.setAttributeNode(dxcodeAttr);
         }
         
         if (labTest.getLabTest() != null) {
            Attr labTestIdAttr = doc.createAttribute("labTestId");
      	   labTestIdAttr.setValue(labTest.getLabTest().getId());
      	   labTestElement.setAttributeNode(labTestIdAttr);
         }
         
         allLabTestsElement.appendChild(labTestElement);
      }
      
      aptElement.appendChild(allLabTestsElement);
      
      return aptElement;
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