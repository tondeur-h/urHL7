


/**
 *
 * @author tondeur-h
 */
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.urhl7.hl7.HL7Location;
import org.urhl7.hl7.HL7Structure;
import org.urhl7.spark.*;
import org.urhl7.utils.StringHelper;

public class TesturHL7 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new TesturHL7();
        
    }

    public TesturHL7() 
    {
        
        try {
            SparkFileReader sfr=new SparkFileReader("C:\\tmp\\test.hl7");
            HL7Structure hl7s=sfr.parse();
            
            System.out.println("Nb segment "+hl7s.getSegments().size());
            
            //for (int i=0;i<hl7s.getSegments().size();i++)
            //{
            //System.out.println("Segment "+hl7s.getSegment(i).getSegmentName());
            //System.out.println("Segment "+hl7s.getSegment(i).toString());
            //System.out.println(hl7s.getSegment(i).getRepeatingFields().size());
              //  for (int j=0;j<hl7s.getSegment(i).getRepeatingFields().size();j++)
                //{
                  //  System.out.println(hl7s.getSegment(i).getRepeatingFields().get(j));
                //}
            //}
            
            //System.out.println(hl7s.get(HL7Location.parse("PID-3")).toString());
            
            StringHelper sh=new StringHelper();
           
            String[] sf=sh.explode(hl7s.get(HL7Location.parse("PID-3")).toString(),"^");
            
            
            System.out.println(sf[0]);
            
        } catch (IOException ex) {
            Logger.getLogger(TesturHL7.class.getName()).log(Level.SEVERE, null, ex);
        }

        
    
    }
    
    
    
    
}
