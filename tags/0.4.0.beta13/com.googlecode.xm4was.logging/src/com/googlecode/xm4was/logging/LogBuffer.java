package com.googlecode.xm4was.logging;

final class LogBuffer {
    private final LogMessage[] buffer = new LogMessage[1024];
    private int head;
    // We start at System.currentTimeMillis to make sure that the sequence is strictly increasing
    // even across server restarts
    private long initialSequence;
    private long nextSequence;

    LogBuffer() {
        initialSequence = System.currentTimeMillis();
        nextSequence = initialSequence;
    }
    
    synchronized void put(LogMessage message) {
        message.setSequence(nextSequence++);
        buffer[head++] = message;
        if (head == buffer.length) {
            head = 0;
        }
        notifyAll();
    }

    synchronized long getNextSequence() {
        return nextSequence;
    }
    
    /**
     * Get log messages from the buffer. The method returns all messages with a sequence number
     * greater than or equal to a given sequence number. It can be instructed to wait until at least
     * one message is available.
     * 
     * @param startSequence
     *            the sequence number of the first message to retrieve
     * @param timeout
     *            the maximum time in milliseconds to wait to return at least one message, or -1 if
     *            the method should never wait for messages to be available
     * @return an array of messages; the array is empty if no message is available and
     *         <code>timeout</code> is -1 or the timeout has expired
     * @throws InterruptedException
     *             if the thread was interrupted while waiting for a message to be available
     */
    synchronized LogMessage[] getMessages(long startSequence, long timeout) throws InterruptedException {
        if (startSequence < initialSequence) {
            startSequence = initialSequence;
        }
        int bufferSize = buffer.length;
        while (true) {
            int position;
            long longCount = nextSequence-startSequence;
            int count;
            if (longCount > bufferSize) {
                position = head;
                count = bufferSize;
            } else {
                count = (int)longCount;
                position = (head+bufferSize-count) % bufferSize;
            }
            if (count == 0 && timeout != -1) {
                wait(timeout);
                continue;
            }
            LogMessage[] messages = new LogMessage[count];
            for (int i=0; i<count; i++) {
                messages[i] = buffer[position++];
                if (position == bufferSize) {
                    position = 0;
                }
            }
            return messages;
        }
    }
}
