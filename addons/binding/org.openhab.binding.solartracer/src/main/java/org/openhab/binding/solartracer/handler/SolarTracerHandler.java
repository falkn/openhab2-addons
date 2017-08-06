/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.solartracer.handler;

import static org.openhab.binding.solartracer.SolarTracerBindingConstants.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TooManyListenersException;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.solartracer.SolarTracerBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 * The {@link SolarTracerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Christian Falk - Initial contribution
 */
public class SolarTracerHandler extends BaseThingHandler
        implements SerialPortEventListener {

    private final Logger logger = LoggerFactory
            .getLogger(SolarTracerHandler.class);

    // Protocol constants
    private static final int PORT_OPEN_TIMEOUT_SEC = 10000;
    private static final int BAUD = 9600;
    private static final byte[] SYNC_HEADER = { (byte) 0xEB, (byte) 0x90,
            (byte) 0xEB, (byte) 0x90, (byte) 0xEB, (byte) 0x90 };
    private static final int MAX_MESSAGE_LENGTH = 200;
    private static final byte QUERY_COMMAND = (byte) 0xA0;

    // It looks like the CRC checksum retrieved on reads does not match the
    // documentation, both xxv/tracer and dangowrt/tracertools does implement
    // this CRC, but never rejects replies that have incorrect CRCs. Let's
    // do the same.
    private static final boolean VALIDATE_READ_CRC = false;

    private SerialPort serialPort;
    private OutputStreamWriter output;
    private InputStream input;
    private String portName;

    // Data reading states
    private ReadState readState = ReadState.WAIT_SYNC;
    private Message msg = new Message();

    public SolarTracerHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId()
                .equals(SolarTracerBindingConstants.CHANNEL_CHARGE_CURRENT)) {
            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public void initialize() {
        portName = (String) getThing().getConfiguration()
                .get(SolarTracerBindingConstants.CONFIG_PORT);

        // Initialize serial port and input/output streams.
        if (portName == null) {
            updateStatus(ThingStatus.OFFLINE,
                    ThingStatusDetail.CONFIGURATION_ERROR,
                    "Serial port name not configured");
            return;
        }

        // Initialize serial port with default configs
        // DATABITS_8, STOPBITS_1 and PARITY_NONE.
        // serialPort = new NRSerialPort(portName, BAUD);
        try {
            serialPort = openSerialPort(portName);
        } catch (IOException e) {
            String msg = "Could not open serial port " + portName
                    + " Exception: " + e;
            updateStatus(ThingStatus.OFFLINE,
                    ThingStatusDetail.COMMUNICATION_ERROR, msg);
            logger.debug(msg);
            return;
        }

        try {
            input = serialPort.getInputStream();
        } catch (IOException e) {
            String msg = "Could not open input stream from serial port "
                    + portName + " Exception: " + e;
            updateStatus(ThingStatus.OFFLINE,
                    ThingStatusDetail.COMMUNICATION_ERROR, msg);
            logger.debug(msg);
            return;
        }

        // TODO: Wait to set online until we got first message?
        updateStatus(ThingStatus.ONLINE);
        readState = ReadState.WAIT_SYNC;
        msg = new Message();

        // } catch (IOException ex) {
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        // "Failed to open input stream from serial port " + portName + " IOException: "
        // + ex.toString());
        // logger.debug("Failed to open input stream from serial port {}. Exception: ", portName,
        // ex.toString());
        // }
        // activate the DATA_AVAILABLE notifier
        try {
            serialPort.addEventListener(this);
        } catch (TooManyListenersException e) {
            throw new RuntimeException("Unable to listen to serial port.", e);
        }
        serialPort.notifyOnDataAvailable(true);

        // output = new OutputStreamWriter(serialPort.getOutputStream());

        logger.info("Serial port [{}] is initialized.", portName);
    }

    private SerialPort openSerialPort(String portName) throws IOException {
        CommPortIdentifier portId = null;
        @SuppressWarnings("rawtypes")
        Enumeration portList = CommPortIdentifier.getPortIdentifiers();
        List<String> allNames = new ArrayList();
        while (portList.hasMoreElements()) {
            CommPortIdentifier id = (CommPortIdentifier) portList.nextElement();
            allNames.add(id.getName() + ":" + id.getPortType());
            if (id.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (id.getName().equals(portName)) {
                    logger.debug("Serial port '{}' has been found.", portName);
                    portId = id;
                }
            }
        }

        if (portId == null) {
            throw new IOException("Serial port " + portName
                    + " not found among " + allNames + ".");
        }

        SerialPort serialPort;
        try {
            serialPort = portId.open("openHAB", PORT_OPEN_TIMEOUT_SEC);
        } catch (PortInUseException e) {
            throw new IOException("Could not open serial port " + portName
                    + ". Exception: " + e.toString(), e);
        }

        try {
            // set port parameters
            serialPort.setSerialPortParams(BAUD, SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        } catch (UnsupportedCommOperationException e) {
            throw new IOException("Could not open configure serial port "
                    + portName + "." + ". Exception: " + e.toString(), e);
        }

        return serialPort;
    }

    @Override
    public void dispose() {
        try {
            if (input != null) {
                input.close();
                input = null;
            }
        } catch (IOException e) {
            logger.warn("Error closing serial input stream: '" + portName + "'",
                    e);
        }

        if (serialPort != null) {
            serialPort.removeEventListener();
            serialPort.close();
            serialPort = null;
        }

        super.dispose();
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        switch (event.getEventType()) {
            case SerialPortEvent.DATA_AVAILABLE:
                readInput();
            default:
                break;
        }
    }

    private void readInput() {
        byte lastByte;

        try {
            while (input.available() >= readState.waitAvailable) {
                switch (readState) {
                    case WAIT_SYNC:
                        lastByte = (byte) input.read();
                        if (lastByte == SYNC_HEADER[msg.syncOffset]) {
                            msg.syncOffset++;
                        } else if (lastByte == SYNC_HEADER[0]) {
                            msg.syncOffset = 1;
                        } else {
                            msg.syncOffset = 0;
                        }

                        if (msg.syncOffset >= SYNC_HEADER.length) {
                            // Sync header complete!
                            readState = ReadState.READ_HEADER;
                        }

                        break;
                    case READ_HEADER:
                        int header = consumeInt(input, readState.waitAvailable);
                        // Convert java signed byte to unsigned.
                        msg.command = (byte) ((header >> 8) & 0xff);
                        msg.dataLength = header & 0xff;
                        readState = ReadState.READ_MSG;
                        break;
                    case READ_MSG:
                        int dataRead = input.read(msg.data, msg.dataOffset,
                                msg.dataLength - msg.dataOffset);
                        if (dataRead < 0) {
                            continue;
                        }
                        msg.dataOffset += dataRead;
                        if (msg.dataOffset >= msg.dataLength) {
                            // All data has been read
                            readState = ReadState.READ_FOOTER;
                        }
                        break;
                    case READ_FOOTER:
                        int footer = consumeInt(input, readState.waitAvailable);
                        msg.crc = footer >> 8;

                        if ((footer & 0xff) == 0x7f) {
                            readMessage(msg);
                        } else {
                            logger.debug(
                                    "Message did not have correct footer end, "
                                            + "skipping message. Footer: ",
                                    footer);
                        }

                        // Continue with next message
                        readState = ReadState.WAIT_SYNC;
                        msg = new Message();

                        break;

                }
            }
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE,
                    ThingStatusDetail.COMMUNICATION_ERROR,
                    "Failed to read from serial port " + portName
                            + " Exception: " + e.toString());
            logger.error("Failed to read from serial port " + portName
                    + " Exception: " + e.toString());
        }
    }

    int consumeInt(InputStream is, int len) throws IOException {
        int result = 0;
        for (int i = 0; i < len; ++i) {
            result <<= 8;
            result |= is.read();
        }
        return result;
    }

    private void readMessage(Message msg) {

        // Validate CRC checksum
        if (VALIDATE_READ_CRC) {
            int expectedCrc = calcuateCrc16(msg.data, msg.dataLength);
            if (msg.crc != expectedCrc) {
                logger.debug(
                        "Discarding message with invalid CRC: {} expected {}",
                        Integer.toHexString(msg.crc),
                        Integer.toHexString(expectedCrc));
                return;
            }
        }
        logger.debug("Recieved message: {}", msg.data);

        if (msg.command != QUERY_COMMAND) {
            logger.debug("Discarding message, not query command: {}",
                    msg.command);
            return;
        }

        if (msg.dataLength < 23) {
            logger.debug("Discarding too short message {} < {}", msg.dataLength,
                    23);
            return;
        }

        // TODO: Mark offline when too many invalid messages?

        // updateStatus(ThingStatus.ONLINE);

        // Parse message and update channels
        DecimalType battVoltage = toDecimal(msg.data, 0);
        updateState(CHANNEL_BATT_VOLTAGE, battVoltage);
        updateState(CHANNEL_PV_VOLTAGE, toDecimal(msg.data, 2));
        DecimalType loadCurrent = toDecimal(msg.data, 6);
        updateState(CHANNEL_LOAD_CURRENT, loadCurrent);
        updateState(CHANNEL_BATT_OVERDISCHARGE_VOLTAGE, toDecimal(msg.data, 8));
        updateState(CHANNEL_BATT_FULL_VOLTAGE, toDecimal(msg.data, 10));
        updateState(CHANNEL_LOAD_ON, toOnOff(msg.data, 12));
        updateState(CHANNEL_LOAD_OVERLOAD, toOnOff(msg.data, 13));
        updateState(CHANNEL_LOAD_SHORT, toOnOff(msg.data, 14));
        updateState(CHANNEL_BATT_OVERLOAD, toOnOff(msg.data, 16));
        updateState(CHANNEL_BATT_OVERDISCHARGE, toOnOff(msg.data, 17));
        updateState(CHANNEL_BATT_FULL, toOnOff(msg.data, 18));
        updateState(CHANNEL_BATT_CHARGING, toOnOff(msg.data, 19));
        updateState(CHANNEL_BATT_TEMP, toDecimalTemp(msg.data, 20));
        DecimalType chargeCurrent = toDecimal(msg.data, 21);
        updateState(CHANNEL_CHARGE_CURRENT, chargeCurrent);

        // Calculated
        updateState(CHANNEL_CHARGE_POWER, new DecimalType(
                battVoltage.floatValue() * chargeCurrent.floatValue()));
        updateState(CHANNEL_LOAD_POWER, new DecimalType(
                battVoltage.floatValue() * loadCurrent.floatValue()));
    }

    /**
     * 16-bit CRC checksum a'la MT-5
     *
     * Matches documentation implementation, but not what's seen in the
     * wild. Keeping this to be able to send correct messages.
     *
     * @param bytes Bytes of data to calculate checksum for.
     * @param len Reading data[0:len]
     * @return
     */
    private static int calcuateCrc16(byte[] bytes, int len) {
        if (bytes.length < len) {
            throw new RuntimeException("bytes (" + bytes.length
                    + ") shorter than len (" + len + ").");
        }
        int i = 0;
        // 32-bit, holds from high to low:
        // r4, r1, r2, r3
        int crc = 0;

        if (i >= 0) {
            crc |= (bytes[i++] & 0xff) << 16;
        }
        if (i >= 1) {
            crc |= (bytes[i++] & 0xff) << 8;
        }

        for (; i < len; ++i) {
            crc |= bytes[i] & 0xff;
            for (int j = 0; j < 8; ++j) {
                crc <<= 1;
                if ((crc & 0x100_0000) != 0) {
                    crc ^= 0x104100;
                }
            }
        }

        return (crc >> 8) & 0xffff;
    }

    private static DecimalType toDecimal(byte[] bytes, int offset) {
        return new DecimalType(
                (((bytes[offset + 1] & 0xff) << 8) | (bytes[offset] & 0xff))
                        / 100.0);
    }

    private static DecimalType toDecimalTemp(byte[] bytes, int offset) {
        return new DecimalType((bytes[offset] & 0xff) - 30.0);
    }

    private static OnOffType toOnOff(byte[] bytes, int offset) {
        return bytes[offset] > 0 ? OnOffType.ON : OnOffType.OFF;
    }

    // A message read from the input.
    private static class Message {
        int syncOffset = 0;
        byte command;
        int dataLength;
        byte[] data = new byte[MAX_MESSAGE_LENGTH];
        int dataOffset = 0;
        int crc = 0;
    }

    // The current part of a message we are currently reading
    // a message.
    private static enum ReadState {
        WAIT_SYNC(1),
        READ_HEADER(3),
        READ_MSG(1),
        READ_FOOTER(3);

        public int waitAvailable;

        ReadState(int waitAvailable) {
            this.waitAvailable = waitAvailable;
        }
    }
}
