package epics.common;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public interface IMessage {

    public enum MessageType{ ErrorBadDestinationAddress, TransferObject, AskIfCanTrack, ResponseToAskIfCanTrack, AskIfTracked, AskConfidence, StartTracking, StopSearch, StartSearch, Found, FoundGlobal };

    
    public MessageType getType();
    public String getFrom();
    public String getTo();
    public Object getContent();

}
