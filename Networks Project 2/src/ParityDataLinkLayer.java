// =============================================================================
// IMPORTS
import java.lang.Thread;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
// =============================================================================


// =============================================================================
/**
 * @file   ParityDataLinkLayer.java
 * @author Scott F. Kaplan (sfkaplan@cs.amherst.edu)
 * @date   February 2020
 *
 * A data link layer that uses start/stop tags and byte packing to frame the
 * data, and that performs error management with a parity bit.  It employs no
 * flow control; damaged frames are dropped.
 */



public class ParityDataLinkLayer extends DataLinkLayer {
// =============================================================================
	// Used in my sendNextFrame to hold onto the last frame that was created from the bitBuffer 
	private Queue<Byte> lastFrameMade = new LinkedList<Byte>();
	// Used in createFrame to add the frame number send to send
	private int currentFrameNumber;
	// gotten from createFrame
	// Used in checkTimeout to see if the right ack has been gotten
	private int ackWaiting; 
	// used in finishframeSend to tell the receiver what frameNum they are 
	// looking for in before sending to the data up to the client
	private int expectedFrameNum;
	// this is gotten from process frame is compared to ackWaiting
	// is used to in checkTimeout to see if a the next data can be framed and sent 
	private int lastAckRecieved;
	// Used in finishFrameSend to track what 

 
    // =========================================================================
    /**
     * Embed a raw sequence of bytes into a framed sequence.
     *
     * @param  data The raw sequence of bytes to be framed.
     * @return A complete frame.
     */
    protected Queue<Byte> createFrame (Queue<Byte> data) {
    
    // ADD THE CURRENT FRAME NUMBER BYTE TO THE RECIEVED DATA 
    // AND USE IT TO CALCULATE THE PARITY
    	
	// Calculate the parity.
	byte parity = calculateParity(data);
	
	// Begin with the start tag.
	Queue<Byte> framingData = new LinkedList<Byte>();
	framingData.add(startTag);

	// Add each byte of original data.
        for (byte currentByte : data) {

	    // If the current data byte is itself a metadata tag, then precede
	    // it with an escape tag.
	    if ((currentByte == startTag) ||
		(currentByte == stopTag) ||
		(currentByte == escapeTag)) {

		framingData.add(escapeTag);

	    }

	    // Add the data byte itself.
	    framingData.add(currentByte);

	}

	// Add the parity byte.
	framingData.add(parity);
	// ADD THE FRAME NUMBER BYTE TO THE FRAME DATA HERE
	ackWaiting = currentFrameNumber%4;
	framingData.add((byte) (ackWaiting));
	currentFrameNumber = currentFrameNumber+1;
	// End with a stop tag.
	framingData.add(stopTag);

	return framingData;
	
    } // createFrame ()
    // =========================================================================


    
    // =========================================================================
    /**
     * Determine whether the received, buffered data constitutes a complete
     * frame.  If so, then remove the framing metadata and return the original
     * data.  Note that any data preceding an escaped start tag is assumed to be
     * part of a damaged frame, and is thus discarded.
     *
     * @return If the buffer contains a complete frame, the extracted, original
     * data; <code>null</code> otherwise.
     */
    protected Queue<Byte> processFrame () {

	// Search for a start tag.  Discard anything prior to it.
	boolean        startTagFound = false;
	Iterator<Byte>             i = receiveBuffer.iterator();
	
	// which means that it is an acknowledgment frame that should used to
	// update a private variable that is used to move finishFrameSend along
	// else, do below
	while (!startTagFound && i.hasNext()) {
	    byte current = i.next();
	    // if this is an acknowledgment frame, set the recieved ack
	    // end the method
	    if(current == ackFrameTag) {
	    	i.remove();
	    	lastAckRecieved = i.next();
	    	return null;
	    }
	    if (current != startTag) {
		i.remove();
	    } else {
		startTagFound = true;
	    }
	}

	// If there is no start tag, then there is no frame.
	if (!startTagFound) {
	    return null;
	}
	
	// Try to extract data while waiting for an unescaped stop tag.
        int                       index = 1;
	LinkedList<Byte> extractedBytes = new LinkedList<Byte>();
	boolean            stopTagFound = false;
	while (!stopTagFound && i.hasNext()) {

	    // Grab the next byte.  If it is...
	    //   (a) An escape tag: Skip over it and grab what follows as
	    //                      literal data.
	    //   (b) A stop tag:    Remove all processed bytes from the buffer and
	    //                      end extraction.
	    //   (c) A start tag:   All that precedes is damaged, so remove it
	    //                      from the buffer and restart extraction.
	    //   (d) Otherwise:     Take it as literal data.
	    byte current = i.next();
            index += 1;
	    if (current == escapeTag) {
		if (i.hasNext()) {
		    current = i.next();
                    index += 1;
		    extractedBytes.add(current);
		} else {
		    // An escape was the last byte available, so this is not a
		    // complete frame.
		    return null;
		}
	    } else if (current == stopTag) {
		cleanBufferUpTo(index);
		stopTagFound = true;
	    } else if (current == startTag) {
		cleanBufferUpTo(index - 1);
                index = 1;
		extractedBytes = new LinkedList<Byte>();
	    } else {
		extractedBytes.add(current);
	    }

	}

	// If there is no stop tag, then the frame is incomplete.
	if (!stopTagFound) {
	    return null;
	}

	if (debug) {
	    System.out.println("ParityDataLinkLayer.processFrame(): Got whole frame!");
	}
        
	// The final byte inside the frame is the parity.  Compare it to a
	// recalculation.
	
	byte receivedParity   = extractedBytes.remove(extractedBytes.size() - 2/* changed this to -2 to account for added frame number byte */);
	// frame number byte removed to recalculate parity
	byte removedFrameNumber = extractedBytes.remove(extractedBytes.size() - 1);
	// reca
	byte calculatedParity = calculateParity(extractedBytes);
	// removed frame number is readded into the extracted bytes to be used in finishFrameSend
	extractedBytes.add(removedFrameNumber);
	if (receivedParity != calculatedParity) {
	    System.out.printf("ParityDataLinkLayer.processFrame():\tDamaged frame\n");
	    return null;
	}

	return extractedBytes;

    } // processFrame ()
    // =========================================================================



    // =========================================================================
    /**
     * After sending a frame, do any bookkeeping (e.g., buffer the frame in case
     * a resend is required).
     *
     * @param frame The framed data that was transmitted.
     */ 
    // called in go(); and checkTimeout(); 
    // transmits data and makes thread wait so receiver can call finishFrameSend
    protected void finishFrameSend (Queue<Byte> framedData) {

    	transmit(framedData);
    	
    	try {
			Thread.sleep(2500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    	
    } // finishFrameSend ()
    // =========================================================================



    // =========================================================================
    /**
     * After receiving a frame, do any bookkeeping (e.g., deliver the frame to
     * the client, if appropriate) and responding (e.g., send an
     * acknowledgment).
     *
     * @param frame The frame of bytes received.
     */
    protected void finishFrameReceive (Queue<Byte> frame) {
    	
    	// Whatever it gets, it sends an acknowledgment frame back,
    	// Only sends frame up to the Host if it is the correct frame number
    	
    	// casts frame back to LinkedList from queue to remove the ackByte
    	LinkedList<Byte> getFrameNum = new LinkedList<Byte>();
    	getFrameNum = (LinkedList<Byte>) frame;
    	byte frameNum = getFrameNum.remove(getFrameNum.size() - 1);
    	
    	LinkedList<Byte> ackFrame = new LinkedList<Byte>();
    	// transmit the acknowledgment frame back to the sender's processFrame
    	ackFrame.add(ackFrameTag);
    	ackFrame.add(frameNum);
    	transmit(ackFrame);
    	// Deliver frame to the client if it is the correct frame number 
    	if(frameNum == (expectedFrameNum%4)) {
    		byte[] deliverable = new byte[getFrameNum.size()];
            for (int i = 0; i < deliverable.length; i += 1) {
                deliverable[i] = getFrameNum.remove();
            }
            client.receive(deliverable);
            expectedFrameNum = expectedFrameNum + 1;
        } 
        
    } // finishFrameReceive ()
    // =========================================================================

    // =========================================================================
    /**
     * Determine whether a timeout should occur and be processed.  This method
     * is called regularly in the event loop, and should check whether too much
     * time has passed since some kind of response is expected.
     */
    protected void checkTimeout () {
    	while (ackWaiting != lastAckRecieved) {
    		finishFrameSend(lastFrameMade);
    		receive();
    		 if (receiveBuffer.peek() != null) {
                 processFrame();
    		 }
    	}
    } // checkTimeout ()
    // =========================================================================



    // =========================================================================
    /**
     * For a sequence of bytes, determine its parity.
     *
     * @param data The sequence of bytes over which to calculate.
     * @return <code>1</code> if the parity is odd; <code>0</code> if the parity
     *         is even.
     */
    private byte calculateParity (Queue<Byte> data) {

	int parity = 0;
	for (byte b : data) {
	    for (int j = 0; j < Byte.SIZE; j += 1) {
		if (((1 << j) & b) != 0) {
		    parity ^= 1;
		}
	    }
	}

	return (byte)parity;
	
    } // calculateParity ()
    // =========================================================================
    


    // =========================================================================
    /**
     * Remove a leading number of elements from the receive buffer.
     *
     * @param index The index of the position up to which the bytes are to be
     *              removed.
     */
    private void cleanBufferUpTo (int index) {

        for (int i = 0; i < index; i += 1) {
            receiveBuffer.remove();
        }

    } // cleanBufferUpTo ()
    // =========================================================================



    // =========================================================================
   
    @Override
    protected Queue<Byte> sendNextFrame () {
    	//
    if (sendBuffer.isEmpty()) {
        return null;
    }
        
	// Extract a frame-worth of data from the sending buffer.
	int frameSize = ((sendBuffer.size() < MAX_FRAME_SIZE)
			 ? sendBuffer.size()
			 : MAX_FRAME_SIZE);
	Queue<Byte> data = new LinkedList<Byte>();
	for (int j = 0; j < frameSize; j += 1) {
	    data.add(sendBuffer.remove());
	}

	// Create a frame from the data and transmit it.
	lastFrameMade = createFrame(data);
    return lastFrameMade;
    
    }
    // DATA MEMBERS

    /** The start tag. */
    private final byte startTag  = (byte)'{';

    /** The stop tag. */
    private final byte stopTag   = (byte)'}';

    /** The escape tag. */
    private final byte escapeTag = (byte)'\\';
    
    /** The acknowledgment frame tag. */
    private final byte ackFrameTag = (byte) 9374957;
    // =========================================================================
   




// =============================================================================
} // class ParityDataLinkLayer
// =============================================================================

