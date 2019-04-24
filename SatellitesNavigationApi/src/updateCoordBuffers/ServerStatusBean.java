package updateCoordBuffers;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * Session Bean implementation class TestBean
 */
@Stateless
@LocalBean
public class ServerStatusBean {

    /**
     * Default constructor. 
     */
    public ServerStatusBean() {
        // TODO Auto-generated constructor stub
    }
    
    public boolean getServerStatus() {
    	return Main.startBufferDone;
    }

}
