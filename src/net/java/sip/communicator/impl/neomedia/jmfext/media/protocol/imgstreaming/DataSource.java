/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.imgstreaming;

import java.awt.*;
import java.io.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.control.*;

import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.util.*;

/**
 * DataSource for our image streaming (which is used for 
 * Desktop streaming).
 *
 * @author Sebastien Vincent
 * @author Lubomir Marinov
 * @author Damian Minkov
 */
public class DataSource
    extends PushBufferDataSource
    implements CaptureDevice
{
    /**
     * The <tt>Logger</tt>.
     */
    private static final Logger logger = Logger.getLogger(DataSource.class);

    /**
     * <tt>DataSource</tt> connection state.
     */
    private boolean connected = false;

    /**
     * <tt>DataSource</tt> start state.
     */
    private boolean started = false;

    /**
     * The JMF controls (which are likely of type <tt>Control</tt>) available
     * for this <tt>DataSource</tt>.
     */
    private final Object[] controls = { new FormatControlImpl() };

    /**
     * Image stream.
     */
    private ImageStream stream = null;

    /**
     * The value of the <tt>streams</tt> property of <tt>DataSource</tt> which
     * represents an empty array of <tt>PushBufferStream</tt>s i.e. no
     * <tt>ImageStream</tt>s in <tt>DataSource</tt>. Explicitly defined in
     * order to reduce unnecessary allocations.
     */
    private static final PushBufferStream[] EMPTY_STREAMS = 
        new PushBufferStream[0];

    /**
     * Resolution supported for image.
     */
    private static final Dimension res[] = new Dimension[] {
        new Dimension(128,96),
        new Dimension(176, 144),
        new Dimension(320, 240),
        new Dimension(352, 288),
        new Dimension(704, 576),
        new Dimension(720, 480),
    };

    /**
     * Array of supported formats.
     */
    private static final Format formats[];

    static
    {
        /* initialize supported format array */
        int i = 0;

        formats = new Format[res.length];
        for(Dimension dim : res)
        {
            formats[i]
                = new RGBFormat(
                        dim, /* resolution */
                        (int)(dim.getWidth() * dim.getHeight() * 4), /* max data length */
                        Format.byteArray, /* format */
                        -1.0f, /* frame rate */
                        32, /* color is coded in 24 bit/pixel */
                        1,
                        2,
                        3); /* color masks (red, green, blue) */
            i++;
        }
    }

    /**
     * Constructor.
     */
    public DataSource()
    {
    }

    /**
     * Constructor.
     *
     * @param locator associated <tt>MediaLocator</tt>
     */
    public DataSource(MediaLocator locator)
    {
        setLocator(locator);
    }

    /**
     * Get supported formats.
     *
     * @return supported formats
     */
    public static Format[] getFormats()
    {
        return formats;
    }

    /**
     * Get the JMF streams.
     *
     * @return streams (one element in image streaming case)
     */
    public PushBufferStream[] getStreams()
    {
        if(stream == null)
        {
            stream = new ImageStream(getLocator());
            /* XXX allow to select other format */
            stream.setFormat(getFormats()[5]);
        }

        return (stream == null) ? EMPTY_STREAMS : 
            new PushBufferStream[] {stream};
    }

    /**
     * Initialize <tt>DataSource</tt>.
     *
     * @throws IOException if initialization problem occurred
     */
    public void connect() throws IOException
    {
        if(connected)
        {
            return;
        }

        connected = true;
    }

    /**
     * Disconnect datasource.
     */
    public void disconnect()
    {
        connected = false;
    }

    /**
     * Get content type.
     *
     * @return RAW content type
     */
    public String getContentType()
    {
        return ContentDescriptor.RAW;
    }

    /**
     * Get duration for this source which is unknown.
     *
     * @return DURATION_UNKNOWN
     */
    public Time getDuration()
    {
        return DURATION_UNKNOWN;
    }

    /**
     * Gives control information to the caller.
     *
     * @return the collection of object controls.
     */
    public Object[] getControls()
    {
        /*
         * The field controls is private so we cannot directly return it.
         * Otherwise, the caller will be able to modify it.
         */
        return controls.clone();
    }

    /**
     * Return required control from the Control[] array
     * if exists.
     *
     * @param controlType the control we are interested in.
     * @return the object that implements the control, or null if not found
     */
    public Object getControl(String controlType)
    {
        return AbstractControls.getControl(this, controlType);
    }

    /**
     * Get the <tt>CaptureDeviceInfo</tt> associated
     * with this datasource.
     *
     * @return <tt>CaptureDeviceInfo</tt> associated
     */
    public CaptureDeviceInfo getCaptureDeviceInfo()
    {
        MediaLocator locator = getLocator();

        return
            new CaptureDeviceInfo(
                    locator.getRemainder(),
                    locator,
                    getFormatControls()[0].getSupportedFormats());
    }

    /**
     * Get supported <tt>FormatControl</tt>.
     *
     * @return array of supported <tt>FormatControl</tt>
     */
    public FormatControl[] getFormatControls()
    {
        return AbstractFormatControl.getFormatControls(this);
    }

    /**
     * Start capture.
     *
     * @throws IOException
     */
    public void start() throws IOException
    {
        /* DataSource already started, do not care */
        if(started)
        {
            return;
        }

        if(!connected)
        {
            throw new IOException("DataSource must be connected!");
        }

        stream.start();
        started = true;
    }

    /**
     * Stop capture.
     */
    public void stop()
    {
        if(started)
        {
            started = false;
            stream.stop();
        }
    }

    /**
     * Implementation of <tt>FormatControl</tt> for this <tt>DataSource</tt> instance.
     *
     * @author Sebastien Vincent
     */
    private class FormatControlImpl
        extends AbstractFormatControl
    {
        /**
         * Current format used.
         */
        private Format format = formats[0];

        /**
         * Set the format used.
         *
         * @param format format to use
         * @return format used or null if format is not supported
         */
        @Override
        public Format setFormat(Format format)
        {
            Format f = AbstractFormatControl.setFormat(this, format);

            if(f != null)
                this.format = f;
            return f;
        }

        /**
         * Get current format used.
         *
         * @return the <tt>Format</tt> of this <tt>DataSource</tt>
         */
        public Format getFormat()
        {
            return format;
        }

        /**
         * Get supported formats.
         *
         * @return an array of <tt>Format</tt> element type which lists the JMF
         * formats supported by this <tt>DataSource</tt> i.e. the ones in which
         * it is able to output
         */
        public Format[] getSupportedFormats()
        {
            return formats.clone();
        }
    }
}
