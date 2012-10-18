/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package epics.camsim.core;

import epics.common.IMessage;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class Message implements IMessage {

    private String from;
    private String to;

    private MessageType msgType;
    private Object content;

    private void init( String from, String to, MessageType msgType, Object content ){
        this.from = from;
        this.to = to;
        this.msgType = msgType;
        this.content = content;
    }

    public Message( String from, String to, MessageType msgType, Object content ){
        this.init( from, to, msgType, content );
    }

    public Object getContent(){
        return this.content;
    }

    public String getFrom() {
        return this.from;
    }

    public String getTo() {
        return this.to;
    }

    public MessageType getType() {
        return this.msgType;
    }

}
