package service;

import java.util.*;
import components.data.*;
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
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("services")
public class LAMSAppointmentService {

   private final String INTRO_MSG = "Welcome to the LAMS Appointment Service";
   private final String SERVICE_DIR = "services/Appointments/";

   @Context
   private UriInfo context;

   private DBSingleton dbSingleton;

   @GET
   @Produces("application/xml")
   public String initialize() throws Exception {
      dbSingleton = DBSingleton.getInstance();
      dbSingleton.db.initialLoad("LAMS");
      
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      StringWriter writer = new StringWriter();

	   Document doc = docBuilder.newDocument();
      Element rootElement = doc.createElement("AppointmentList");
      doc.appendChild(rootElement);
      
      Element introElement = doc.createElement("intro");
      introElement.appendChild(doc.createTextNode(INTRO_MSG));
		rootElement.appendChild(introElement);
      
      Element wadlElement = doc.createElement("wadl");
      wadlElement.appendChild(doc.createTextNode(this.context.getBaseUri().toString() + "application.wadl"));
		rootElement.appendChild(wadlElement);
      
      transformer.transform(new DOMSource(doc), new StreamResult(writer));
      return writer.toString();
   }
   
   @Path("Appointments")
   @GET
   @Produces("application/xml")
   public String getAllAppointments() throws Exception {
      List<Object> objs;
      
      dbSingleton = DBSingleton.getInstance();
      objs = dbSingleton.db.getData("Appointment", "");
      
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
            aptElement = addUriTag(aptElement, doc, this.context.getBaseUri().toString() + SERVICE_DIR, appointment.getId());
            
            //Add attributes to appointment tag.
            aptElement = addAptAttr(appointment, aptElement, doc);
            
            //Add patient data.
            aptElement = addPatientToApt(appointment, aptElement, doc, this.context.getBaseUri().toString() + SERVICE_DIR);
            
            //Add phlebotomist data.
            aptElement = addPhlebotomistToApt(appointment, aptElement, doc, this.context.getBaseUri().toString() + SERVICE_DIR);
            
            //Add PSC data.
            aptElement = addPSCToApt(appointment, aptElement, doc, this.context.getBaseUri().toString() + SERVICE_DIR);
            
            //Add Lab Test data.
            aptElement = addLabTestsToApt(appointment, aptElement, doc, this.context.getBaseUri().toString() + SERVICE_DIR);
            
            rootElement.appendChild(aptElement);
         }
         
         transformer.transform(new DOMSource(doc), new StreamResult(writer));
         result += writer.toString();
      }
      
      return result;
   }
   
   @Path("Appointments/{appointmentNumber}")
   @GET
   @Produces("application/xml")
   public String getAppointment(@PathParam("appointmentNumber") String appointmentNumber) throws Exception {
      List<Object> objs;
      String arg = "id='" + appointmentNumber + "'";
      
      dbSingleton = DBSingleton.getInstance();
      objs = dbSingleton.db.getData("Appointment", arg);
      
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
         errElement.appendChild(doc.createTextNode("ERROR: Appointment is not available"));
		   rootElement.appendChild(errElement);
      } else if (objs != null) {
         Appointment appointment = (Appointment) objs.get(0);
         Element aptElement = doc.createElement("appointment");
         aptElement = addUriTag(aptElement, doc, this.context.getBaseUri().toString() + SERVICE_DIR, appointment.getId());
         
         //Add attributes to appointment tag.
         aptElement = addAptAttr(appointment, aptElement, doc);
         
         //Add patient data.
         aptElement = addPatientToApt(appointment, aptElement, doc, this.context.getBaseUri().toString() + SERVICE_DIR);
         
         //Add phlebotomist data.
         aptElement = addPhlebotomistToApt(appointment, aptElement, doc, this.context.getBaseUri().toString() + SERVICE_DIR);
         
         //Add PSC data.
         aptElement = addPSCToApt(appointment, aptElement, doc, this.context.getBaseUri().toString() + SERVICE_DIR);
         
         //Add Lab Test data.
         aptElement = addLabTestsToApt(appointment, aptElement, doc, this.context.getBaseUri().toString() + SERVICE_DIR);
         
         rootElement.appendChild(aptElement);
      }
      
      transformer.transform(new DOMSource(doc), new StreamResult(writer));
      result += writer.toString();
      
      return result;
   }
   
   @Path("Appointments")
   @PUT
   @Consumes({"text/xml","application/xml"})
   @Produces("application/xml")
   public String addAppointment(String xmlStyle) throws Exception {
      String aptId = createAppointmentId();
      
      Document doc = parseXmlFromString(xmlStyle);
      doc.getDocumentElement().normalize();
      
      Node node = doc.getElementsByTagName("appointment").item(0);
      Element appointment = (Element) node;
      
      String date = appointment.getElementsByTagName("date").item(0).getTextContent();
      String time = appointment.getElementsByTagName("time").item(0).getTextContent() + ":00";
      String physicianId = appointment.getElementsByTagName("physicianId").item(0).getTextContent();
      String pscId = appointment.getElementsByTagName("pscId").item(0).getTextContent();
      String patientId = appointment.getElementsByTagName("patientId").item(0).getTextContent();
      String phlebotomistId = appointment.getElementsByTagName("phlebotomistId").item(0).getTextContent();
      
      //Check physician id.
      if (!checkPhysicianId(physicianId)) {
         return generateErrorMsg();
      }
      
      //Check patient id.
      if (!checkPatientId(patientId)) {
         generateErrorMsg();
      }
      
      //Check phlebotomist id.
      if (!checkPhlebotomistId(phlebotomistId)) {
         generateErrorMsg();
      }
      
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
      
      //Check phlebotomist id.
      if (!checkLabTests(labTests)) {
         generateErrorMsg();
      }
      
      Appointment newAppt = new Appointment(aptId,java.sql.Date.valueOf(date),java.sql.Time.valueOf(time));
      newAppt.setAppointmentLabTestCollection(labTests);
      newAppt.setPatientid(patient);
      newAppt.setPhlebid(phlebotomist);
      newAppt.setPscid(psc);
      
      dbSingleton.db.addData(newAppt);
      
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	   DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      StringWriter writer = new StringWriter();

	   Document returnDoc = docBuilder.newDocument();
      Element rootElement = returnDoc.createElement("AppointmentList");
      rootElement = addUriTag(rootElement, returnDoc, this.context.getBaseUri().toString() + SERVICE_DIR, aptId);
      
      transformer.transform(new DOMSource(returnDoc), new StreamResult(writer));
      return writer.toString();
   }
   
   private Patient getPatient(String patientId) {
      List<Object> result = dbSingleton.db.getData("Patient", "id='" + patientId + "'");
      return result == null || result.isEmpty() ? null : (Patient) result.get(0);
   }
   
   private Physician getPhysician(String physicianId) {
      List<Object> result = dbSingleton.db.getData("Physician", "id='" + physicianId + "'");
      return result == null || result.isEmpty() ? null : (Physician) result.get(0);
   }
   
   private Phlebotomist getPhlebotomist(String phlebotomistId) {
      List<Object> result = dbSingleton.db.getData("Phlebotomist", "id='" + phlebotomistId + "'");
      return result == null || result.isEmpty() ? null : (Phlebotomist) result.get(0);
   }
   
   private PSC getPSC(String PSCId) {
      List<Object> result = dbSingleton.db.getData("PSC", "id='" + PSCId + "'");
      return result == null || result.isEmpty() ? null : (PSC) result.get(0);
   }
   
   private LabTest getLabTest(String labTestId) {
      List<Object> result = dbSingleton.db.getData("LabTest", "id='" + labTestId + "'");
      return result == null || result.isEmpty() ? null : (LabTest) result.get(0);
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
      
      return String.valueOf(highestId + 10);
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
   
   private Element addPatientToApt(Appointment appointment, Element aptElement, Document doc, String baseUri) {
      Element patientElement = doc.createElement("patient");
      patientElement = addUriTag(patientElement, doc, baseUri, null);
      
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
   
   private Element addPhlebotomistToApt(Appointment appointment, Element aptElement, Document doc, String baseUri) {
      Element phlebElement = doc.createElement("phlebotomist");
      phlebElement = addUriTag(phlebElement, doc, baseUri, null);
      
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
   
   private Element addPSCToApt(Appointment appointment, Element aptElement, Document doc, String baseUri) {
      Element pscElement = doc.createElement("psc");
      pscElement = addUriTag(pscElement, doc, baseUri, null);
      
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
   
   private Element addLabTestsToApt(Appointment appointment, Element aptElement, Document doc, String baseUri) {
      Element allLabTestsElement = doc.createElement("allLabTests");
      
      List<AppointmentLabTest> labTests = appointment.getAppointmentLabTestCollection();
      
      for (int i = 0; i < labTests.size(); i++) {
         AppointmentLabTest labTest = labTests.get(i);
         
         Element labTestElement = doc.createElement("appointmentLabTest");
         labTestElement = addUriTag(labTestElement, doc, baseUri, null);
      
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
   
   private Element addUriTag(Element elem, Document doc, String baseUri, String uriAppendage) {
      Element uriElement = doc.createElement("uri");
      
      if (uriAppendage != null) {
         uriElement.appendChild(doc.createTextNode(baseUri + uriAppendage));
      }
      
		elem.appendChild(uriElement);
      
      return elem;
   }
   
   private String generateErrorMsg() throws Exception {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	   DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      StringWriter writer = new StringWriter();

	   Document errDoc = docBuilder.newDocument();
      Element rootElement = errDoc.createElement("AppointmentList");
      errDoc.appendChild(rootElement);
      
      Element errElement = errDoc.createElement("error");
      errElement.appendChild(errDoc.createTextNode("ERROR: Appointment is not available"));
	   rootElement.appendChild(errElement);
      
      transformer.transform(new DOMSource(errDoc), new StreamResult(writer));
      return writer.toString();
   }
   
   private boolean checkPatientId(String patientId) {
      return getPatient(patientId) != null;
   }
   
   private boolean checkPhysicianId(String physicianId) {
      return getPhysician(physicianId) != null;
   }
   
   private boolean checkLabTests(List<AppointmentLabTest> labTests) {
      for (int i = 0; i < labTests.size(); i++) {
         if (getLabTest(labTests.get(i).getLabTest().getId()) == null) {
            return false;
         }
      }
      
      return true;
   }
   
   private boolean checkPhlebotomistId(String phlebotomistId) {
      return getPhlebotomist(phlebotomistId) != null;
   }
}