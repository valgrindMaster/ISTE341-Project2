import service.*;

public class Main {
   public static void main(String[] args) throws Exception {
      LAMSAppointmentService service = new LAMSAppointmentService();
      System.out.println("LAMSAppointmentService created.");
      
      System.out.print("Invoking initialize()...");
      String initResult = service.initialize();
      
      System.out.println("Done.");
      System.out.println("Result is:");
      System.out.println(initResult);
      
      System.out.println("---\n\n");
      
      System.out.print("Invoking getAllAppointments()...");
      String getAllAptResults = service.getAllAppointments();
      
      System.out.println("Done.");
      System.out.println("Result is:");
      System.out.println(getAllAptResults);
      
//       System.out.println("---\n\n");
//       
//       System.out.print("Invoking initialize()...");
//       String initResult = service.initialize();
//       
//       System.out.println("Done.");
//       System.out.println("Result is:");
//       System.out.println(initResult);
   }
}