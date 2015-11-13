package epics.common;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public interface IMessage {

    /**
     * different types of messages
     * 
     * @author Lukas Esterle <lukas [dot] esterle [at] aau [dot] at>
     *
     */
    public enum MessageType{ ErrorBadDestinationAddress, TransferObject, AskIfCanTrack, ResponseToAskIfCanTrack, AskIfTracked, AskConfidence, StartTracking, StopSearch, StartSearch, Found, FoundGlobal };

    
    /**
     * the message type
     * @return
     */
    public MessageType getType();
    
    /**
     * the sender of the message
     * @return
     */
    public String getFrom();
    
    /**
     * the receiver of the message
     * @return
     */
    public String getTo();
    
    /** 
     * the content of the message
     * @return
     */
    public Object getContent();

}
