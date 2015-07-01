/*
This is a part of Linkster

Linkster is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Copyright 2008 Will Chapman
*/
package com.raxware.linkster.ui;

import javax.baja.sys.*;
import javax.baja.gx.*;
import javax.baja.naming.*;
import javax.baja.ui.*;
import javax.baja.ui.enums.*;
import javax.baja.ui.pane.*;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.view.BWbView;

public class BLinksterToolView extends BWbView {
	private BLinksterSide linkFrom;
	private BLinksterSide linkTo;
	private BButton copyButton;
	private BTextEditorPane resultsPane;
	private BButton analyzeButton;
	private BButton linkButton;
	
	private BOrd rootOrd;
	
	public BLinksterToolView(){}	
	
	/**
	 * Sets up the user interface
	 * 
	 * @return BWidget the view in the form of a BWidget (BSplitPane)
	 */
	public BWidget generateContent()
	{		
		linkFrom = new BLinksterSide("Link From");
		linkTo = new BLinksterSide("Link To");
		
		BWbShell shell = BWbShell.getWbShell(this);
		rootOrd = shell.getActiveOrd();
		
		copyButton = new BButton();
		copyButton.setCommand(new CopyInfoCommand(), true, true);
		
		analyzeButton = new BButton();
		analyzeButton.setCommand(new AnalyzeCommand(), true, true);
		
		linkButton = new BButton();
		linkButton.setCommand(new LinkCommand(), true, true);
		linkButton.getCommand().setEnabled(false);
		
		BSplitPane splitPane = new BSplitPane(BOrientation.vertical, 75.0);
		
		BGridPane center = new BGridPane(1);
		center.add(null, copyButton);
		center.add(null, analyzeButton);
		center.add(null, linkButton);
		center.add(null, new BNullWidget());
		center.setStretchRow(3);
		
		BEdgePane pane = new BEdgePane();
		pane.setRight(linkTo);
		pane.setCenter(center);
		pane.setLeft(linkFrom);
		splitPane.setWidget1(pane);
		
		resultsPane = new BTextEditorPane("Results will show here...", 30, 100, false);
		splitPane.setWidget2(resultsPane);
		
		return splitPane;
	}	
	
	public void doUpdate()
	{
		resultsPane.setText("Updating...");
	}
	
	private void addToResultPane(String txt)
	{
		resultsPane.setText(resultsPane.getText()+"\n"+txt);
	}
	
	private void clearResultsPane()
	{
		resultsPane.setText("");
	}
	
	
	protected void doLoadValue(BObject arg0, Context arg1) throws Exception {
		setContent(generateContent());
	}
	
	/**
	 * Once verified, this Command will do the actually
	 * linking of the objects in the lists.
	 * 
	 * @author Will Chapman
	 */
	class LinkCommand extends Command
	{
		public LinkCommand()
		{
			super(
					linkButton,
					"Link",
					BImage.make(BIcon.std("link.png")),
					null,
					"Actually performs link"
				);
		}
		public CommandArtifact doInvoke()
		{
			clearResultsPane();
			int count = linkFrom.getList().getItemCount();
			
			addToResultPane("Linking "+count+" objects now...");
			for(int i=0; i < count; i++)
			{
			  // get the ord
				String fromOrdString = (String) linkFrom.getList().getItem(i);
				String toOrdString = (String) linkTo.getList().getItem(i);
				
				String fromOrd = fromOrdString.substring(0, fromOrdString.lastIndexOf('.'));
				String toOrd = toOrdString.substring(0, toOrdString.lastIndexOf('.'));
				
				// get the slot
				String fromSlot = fromOrdString.substring(fromOrdString.lastIndexOf('.')+1);
				String toSlot = toOrdString.substring(toOrdString.lastIndexOf('.')+1);
				
				// get the ord object
				BOrd from = BOrd.make(rootOrd, fromOrd);
				BOrd to = BOrd.make(rootOrd, toOrd);
				
				// the real magic
				BLink link = new BLink(from.get().asComponent().getHandleOrd(), fromSlot, toSlot, true);				
				BComponent toComp = to.resolve().get().asComponent();
				toComp.add("rwLink_"+fromSlot+"_"+toSlot, link);
				// magic is done
				
				addToResultPane(" Link: " + from.toString() + "["+fromSlot+"] -> " + to.toString() + "["+toSlot+"]");
			}
			return null;
		}
	}
	
	/**
	 * This will verify certain parameters in both lists
	 * before starting the linking process.
	 * 
	 * @author Will Chapman
	 *
	 */
	class AnalyzeCommand extends Command
	{
		public AnalyzeCommand()
		{
			super(
				analyzeButton,
				"Analyze",
				BImage.make("module://linkster/com/raxware/linkster/res/icons/clean.png"),
				null,
				"Checks everything before going further in linking"
				);	
		}
		
		/**
		 * So what do I need to analyze?
		 *  * Make sure that both lists are populated
		 *  * Make sure that both lists have equal number of elements
		 * 
		 */
		public CommandArtifact doInvoke()
		{
			clearResultsPane();
			// both are populated
			
			addToResultPane(BAbsTime.now().toString());
			addToResultPane("Analyzing started...");
			try
			{
				bothArePopulated();
				addToResultPane("   Both lists are populated... done");
				
				bothHaveSameAmountOfElements();
				addToResultPane("   Both lists are equal in size... done");
				
				addToResultPane("   Checking generated ords \"From\"...");
				checkOrds(linkFrom);
								
				addToResultPane("   Checking generated ords \"To\"...");
				checkOrds(linkTo);
				
				linkButton.getCommand().setEnabled(true);
				addToResultPane("Check complete");
			}
			catch(Exception e)
			{
				linkButton.getCommand().setEnabled(false); // should already be false - just in case
				addToResultPane("ERROR:  Linking not enabled");
				addToResultPane("REASON: " + e.getMessage());
			}
			return null;
		}
		
		private void checkOrds(BLinksterSide side)
			throws Exception
		{
			int count = side.getList().getItemCount();
						
			
			for(int i=0; i < count; i++)
			{
				String thisOrdString = (String) side.getList().getItem(i);
				
				// the string consists of the ord, followed by a period 
				// then the slot
				String ord = thisOrdString.substring(0, thisOrdString.lastIndexOf('.'));
				String slot = thisOrdString.substring(thisOrdString.lastIndexOf('.')+1);
				if(ord==null || ord.length()<=0 || slot==null || slot.length()<=0)
				  throw new Exception("Could not parse ord ("+thisOrdString+")");
				// end of verifying 
				
				OrdTarget current = BOrd.make(rootOrd, ord).resolve();
				String rwOk = current.canRead() && current.canWrite() ? " [R/W]" : " [Perm Error]";
				String handle = current.get().asComponent().getHandleOrd().toString();
				Slot slotObj = current.get().asComponent().getSlot(slot);
				String slotCheck = (slotObj != null) ? " ["+slot+" ok]" : " ["+slot+" DNE]";
				addToResultPane("    Resolving \""+ord+"\" " + rwOk + " ["+handle+"]"+slotCheck);
			}
		}
		
		private void bothHaveSameAmountOfElements()
			throws Exception
		{
			int one = linkFrom.getList().getModel().getItemCount();
			int two = linkTo.getList().getModel().getItemCount();
			
			if ( one != two )
				throw new Exception("Sides do not match ("+one+" != "+two+")");
			
		}
		
		private void bothArePopulated()
			throws Exception
		{
			if (linkFrom.getList().getItemCount() <= 0 || linkTo.getList().getItemCount() <= 0)
				throw new Exception("One or both of the lists are empty");
		}
	}
	
	/**
	 * As a convienence, it will copy the data in the "from" side to the "to" side
	 * The "to" side will still need to be compiled before linking is enabled.
	 * @author Administrator
	 *
	 */
	class CopyInfoCommand extends Command
	{
		public CopyInfoCommand()
		{
			super(
				copyButton,
				"Copy Info",
				BImage.make("module://linkster/com/raxware/linkster/res/icons/1rightarrow.png"),
				null,
				"Copies the template/range from the left side to the right"
				);
		}
		
		public CommandArtifact doInvoke()
		{
			linkTo.setTemplateText(linkFrom.getTemplate().getText());
			linkTo.setRangeText(linkFrom.getRange().getText());
			linkTo.setSlotText(linkFrom.getSlotText().getText());
			return null;
		}
	}
	
	/*-
	 class BLinksterToolView
	 {
	 } 
	 -*/
/*+ ------------ BEGIN BAJA AUTO GENERATED CODE ------------ +*/
/*@ $com.raxware.linkster.ui.BLinksterToolView(3445711970)1.0$ @*/
/* Generated Thu Apr 24 11:28:58 EDT 2008 by Slot-o-Matic 2000 (c) Tridium, Inc. 2000 */

////////////////////////////////////////////////////////////////
// Type
////////////////////////////////////////////////////////////////
  
  public Type getType() { return TYPE; }
  public static final Type TYPE = Sys.loadType(BLinksterToolView.class);

/*+ ------------ END BAJA AUTO GENERATED CODE -------------- +*/

}
