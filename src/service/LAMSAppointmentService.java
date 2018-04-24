package service;

import java.util.*;
import components.data.*;
import business.*;

public class LAMSAppointmentService {

   private DBSingleton dbSingleton;

   public String initialize() {
      dbSingleton = DBSingleton.getInstance();
      dbSingleton.db.initialLoad("LAMS");
      return "Database Initialized";
   }
   
   public String getAllAppointments() {
      String result = "All appointments:\n";
      List<Object> objs;
      
      dbSingleton = DBSingleton.getInstance();
      objs = dbSingleton.db.getData("Appointment", "");
      
      if (objs == null) {
         dbSingleton.db.initialLoad("LAMS");
         objs = dbSingleton.db.getData("Appointment", "");
      }
      
      if (objs != null) {
         for (Object obj : dbSingleton.db.getData("Appointment", "")) {
            result += obj + "\n";
         }
      }
      
      if (objs == null || objs.isEmpty()) {
         result = "Error: Appointments could not be retrieved.";
      }
      
      return result;
   }
   
   public String getAppointment(String appointmentNumber) {
      String result = "Appointment:\n";
      List<Object> objs;
      
      dbSingleton = DBSingleton.getInstance();
      objs = dbSingleton.db.getData("Appointment", "patientid='" + appointmentNumber + "'");
      
      if (objs == null) {
         dbSingleton.db.initialLoad("LAMS");
         objs = dbSingleton.db.getData("Appointment", "patientid='" + appointmentNumber + "'");
      }
      
      Patient patient = null;
      Phlebotomist phleb = null;
      PSC psc = null;
      
      if (objs != null) {
         for (Object obj : objs){
            result += obj + "\n";
            
            patient = ((Appointment)obj).getPatientid();
            phleb = ((Appointment)obj).getPhlebid();
            psc = ((Appointment)obj).getPscid();
            
            //result += "Patient ID: " + patient + "\n";
            //result += "Phleb ID: " + patient + "\n";
            //result += "PSC ID: " + patient + "\n\n";
         }
      }
      
      if (objs == null || objs.isEmpty()) {
         result = "Error: no appointment records were found.";
      }
      
      return result;
   }
   
   public String addAppointment(String xmlStyle) {
      String result = "Appointment:\n";
   
      System.out.println("^^^^^^^"+phleb.getId());
      Appointment newAppt = new Appointment("800",java.sql.Date.valueOf("2009-09-01"),java.sql.Time.valueOf("10:15:00"));
      //extra steps here due to persistence api and join, need to create objects in list
      List<AppointmentLabTest> tests = new ArrayList<AppointmentLabTest>();
      AppointmentLabTest test = new AppointmentLabTest("800","86900","292.9");
      test.setDiagnosis((Diagnosis)dbSingleton.db.getData("Diagnosis", "code='292.9'").get(0));
      test.setLabTest((LabTest)dbSingleton.db.getData("LabTest","id='86900'").get(0));
      tests.add(test);
      newAppt.setAppointmentLabTestCollection(tests);
      newAppt.setPatientid(patient);
      newAppt.setPhlebid(phleb);
      newAppt.setPscid(psc);
      
      boolean good = dbSingleton.db.addData(newAppt);
      objs = dbSingleton.db.getData("Appointment", "");
      for (Object obj : objs){
      System.out.println(obj);
      System.out.println("");
      }
   }
}