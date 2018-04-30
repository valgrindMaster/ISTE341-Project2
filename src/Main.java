import service.*;

public class Main {
   public static void main(String[] args) throws Exception {
      LAMSAppointmentService service = new LAMSAppointmentService();
      String initResult = service.initialize();
      System.out.println("LAMSAppointmentService created and database initialized.");
      
      System.out.println("Result: ");
      System.out.println(initResult);
      System.out.println("---");
      
      System.out.print("Invoking getAllAppointments()...");
      String getAllAptResults = service.getAllAppointments();
      System.out.println("Done.");
      
      System.out.println("Result is:");
      System.out.println(getAllAptResults);
      
      System.out.println("---");
      
      System.out.print("Invoking getAppointment() with existing appointment id as param...");
      String getApt1Result = service.getAppointment("740");
      System.out.println("Done.");
      
      System.out.println("Result is:");
      System.out.println(getApt1Result);
      
      System.out.println("---");
      
      System.out.print("Invoking getAppointment() with non-existing appointment id as param...");
      String getApt2Result = service.getAppointment("1000");
      System.out.println("Done.");
      
      System.out.println("Result is:");
      System.out.println(getApt2Result);
      
      System.out.println("---");
      
      String apt1Xml = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?><appointment><date>2018-12-28</date><time>10:00</time><patientId>220</patientId><physicianId>20</physicianId><pscId>520</pscId><phlebotomistId>110</phlebotomistId><labTests><test id=\"86900\" dxcode=\"292.9\" /><test id=\"86609\" dxcode=\"307.3\" /></labTests></appointment>";
      
      System.out.print("Invoking addAppointment() with GOOD xml input...");
      String addApt1Result = service.addAppointment(apt1Xml);
      System.out.println("Done.");
      
      System.out.println("Result is:");
      System.out.println(addApt1Result);
      
      System.out.println("---");
      
      String apt2Xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><appointment><date>2018-13-34</date><time>99:00</time><patientId>bad</patientId><physicianId>bad</physicianId><pscId>bad</pscId><phlebotomistId>bad</phlebotomistId><labTests><test id=\"bad\" dxcode=\"000\" /></labTests></appointment>";
      
      System.out.print("Invoking addAppointment() with BAD xml input...");
      String addApt2Result = service.addAppointment(apt2Xml);
      System.out.println("Done.");
      
      System.out.println("Result is:");
      System.out.println(addApt2Result);
   }
}