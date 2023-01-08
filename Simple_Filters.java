import java.awt.*;
import java.awt.event.*;
import java.io.*;
import ij.plugin.frame.*;
import ij.*;
import ij.process.*;
import ij.gui.*;

/* 

   This plugin must be compiled and run from the inside of the ImageJ
   program. 
   Plugins -> Compile and Run... -> ../Simple_Filters.java

   This ImageJ plugin creates a simple graphical user interface with 5
   options to manipulate any desired image that must be accesed through
   ImageJ program.

   Filters that are implemented into this plugin:
    Median Filter - used to reduce noise from the image;
    Contrast Filter - used to lower the contrast of the image;
    Sobel X Filter - used to emphasize the edges on the X axis;
    Sobel Y Filter - used to emphasize the edges on the Y axis;
    Reset - used to reset the image to it's primary state;

 */

public class Simple_Filters extends PlugInFrame implements ActionListener { // Simple_Filters class
    private Panel panel;
    private static Frame frame;
    private int previousID;

    public Simple_Filters() {
	super("Simple Filters");
	if (frame != null) {
	    frame.toFront();
	    return;
	}
	frame = this;
	
	// Puts a key listener on the main ImageJ object.
	addKeyListener(IJ.getInstance());

	setLayout(new FlowLayout());
	panel = new Panel();
	panel.setLayout(new GridLayout(2, 3, 5, 5));

	addButton("Median Filter");
	addButton("Contrast Filter");
	addButton("Sobel X Filter");
	addButton("Sobel Y Filter");
	addButton("Reset");
	add(panel);

	// Lays out the Window class components with the
	// desired dimensions in the window.
	pack();

	// Positions the plugin window in the middle
	// of the screen.
	GUI.center(this);
	setVisible(true);
    }

    void addButton(String label) {
	Button b = new Button(label);
	b.addActionListener(this);

	// Adds a key listener for the buttons on
	// the main ImageJ object.
	b.addKeyListener(IJ.getInstance());
	panel.add(b);
    }

    public void actionPerformed(ActionEvent e) {
	ImagePlus imp = WindowManager.getCurrentImage();
	if (imp == null) {
	    IJ.beep(); // Creates a beep sound.
	    IJ.showStatus("No image."); // Prints a message on the status bar.
	    previousID = 0;
	    return;
	}

	if (!imp.lock()) {
	    previousID = 0;
	    return;
	}

	int id = imp.getID();

	if (id != previousID) {
	    // Returns the pixel copy of an image so it can be reset.
	    imp.getProcessor().snapshot();
	}

	previousID = id;
	String label = e.getActionCommand();

	if (label == null) {
	    return;
	}

	new Runner(label, imp);
    }

    public void processWindowEvent(WindowEvent e) {
	super.processWindowEvent(e);
	if (e.getID() == WindowEvent.WINDOW_CLOSING) {
	    frame = null;
	}
    }

    class Runner extends Thread { // Runner class
	private String command;
	private ImagePlus imp;

	Runner(String command, ImagePlus imp) {
	    super(command);
	    this.command = command;
	    this.imp = imp;
	    setPriority(Math.max(getPriority()-2, MIN_PRIORITY));
	    start();
	}

	public void run() {
	    try {
		runCommand(command, imp);
	    } catch(OutOfMemoryError e) {
		IJ.outOfMemory(command);
		if (imp != null) {
		    imp.unlock();
		} 
	    } catch(Exception e) {
		CharArrayWriter caw = new CharArrayWriter();
		PrintWriter pw = new PrintWriter(caw);
		e.printStackTrace(pw);
		IJ.log(caw.toString());
		IJ.showStatus("");
		if (imp != null) {
		    imp.unlock();
		}
	    }
	}

	void runCommand(String command, ImagePlus imp) {

	    // Returns an ImageProcessor class associated with the current
	    // image. It is used to manipulate the image. If there is no
	    // ImageProcessor associated with the image, then null is returned.
	    ImageProcessor ip = imp.getProcessor();
	    IJ.showStatus(command + "...");
	    long startTime = System.currentTimeMillis();

	    // Returns the current selection on the image. Returns null
	    // if there is no selection present.
	    Roi roi = imp.getRoi();

	    // Returns the selected region (ROI - region of interest) as
	    // the ByteProcessor object.
	    ImageProcessor mask = roi != null ? roi.getMask() : null;
	    
	    if (command.equals("Median Filter")) {
		ip.medianFilter();
	    }

	    else if (command.equals("Contrast Filter")) {
		int[] contrast = new int[] {0, -1, 0, -1, 5, -1, 0, -1, 0};
		ip.convolve3x3(contrast);
	    }

	    else if (command.equals("Sobel X Filter")) {
		int[] sobelX = new int[] {-1, 0, 1, -2, 0, 2, -1, 0, 1};
		ip.convolve3x3(sobelX);
	    }

	    else if (command.equals("Sobel Y Filter")) {
		int[] sobelY = new int[] {-1, -2, -1, 0, 0, 0, 1, 2, 1};
		ip.convolve3x3(sobelY);
	    }

	    else if (command.equals("Reset")) {
		ip.reset();
	    }

	    if (mask != null) {
		ip.reset(mask);
	    }

	    // Updates the image according to the used filter.
	    imp.updateAndDraw();
	    imp.unlock();
	    IJ.showStatus((System.currentTimeMillis() - startTime) + " milliseconds");
	}
    } // Runner class
} // Simple_Filters class
