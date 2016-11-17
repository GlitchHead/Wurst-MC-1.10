/*
 * Copyright � 2014 - 2016 | Wurst-Imperium | All rights reserved.
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package tk.wurst_client.commands;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import tk.wurst_client.ai.PathFinder;
import tk.wurst_client.ai.PathPoint;
import tk.wurst_client.commands.Cmd.Info;
import tk.wurst_client.events.listeners.RenderListener;
import tk.wurst_client.navigator.settings.CheckboxSetting;
import tk.wurst_client.utils.EntityUtils.TargetSettings;

@Info(
	description = "Shows the shortest path to a specific point. Useful for labyrinths and caves.",
	name = "path",
	syntax = {"<x> <y> <z>", "<entity>", "-debug", "-depth", "-refresh"},
	help = "Commands/path")
public class PathCmd extends Cmd implements RenderListener
{
	private PathFinder pathFinder;
	private ArrayList<BlockPos> path;
	private boolean enabled;
	private BlockPos lastPos;
	
	private TargetSettings targetSettings = new TargetSettings()
	{
		@Override
		public boolean targetFriends()
		{
			return true;
		}
		
		@Override
		public boolean targetBehindWalls()
		{
			return true;
		}
	};
	
	public CheckboxSetting debugMode = new CheckboxSetting("Debug mode", false);
	public CheckboxSetting depthTest = new CheckboxSetting("Depth test", false);
	
	public PathCmd()
	{
		settings.add(debugMode);
		settings.add(depthTest);
	}
	
	@Override
	public void execute(String[] args) throws Error
	{
		boolean refresh = false;
		if(args.length > 0 && args[0].startsWith("-"))
			switch(args[0])
			{
				case "-debug":
					debugMode.toggle();
					wurst.chat.message("Debug mode "
						+ (debugMode.isChecked() ? "on" : "off") + ".");
					return;
				case "-depth":
					depthTest.toggle();
					wurst.chat.message("Depth test "
						+ (depthTest.isChecked() ? "on" : "off") + ".");
					return;
				case "-refresh":
					if(lastPos == null)
						error("Cannot refresh: no previous path.");
					refresh = true;
					break;
			}
		
		path = new ArrayList<>();
		
		if(enabled)
		{
			wurst.events.remove(RenderListener.class, this);
			enabled = false;
			
			if(args.length == 0)
				return;
		}
		
		final BlockPos pos;
		if(refresh)
			pos = lastPos;
		else
		{
			int[] posArray = argsToPos(targetSettings, args);
			pos = new BlockPos(posArray[0], posArray[1], posArray[2]);
			lastPos = pos;
		}
		
		Thread thread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				System.out.println("Finding path...");
				long startTime = System.nanoTime();
				pathFinder = new PathFinder(pos);
				if(pathFinder.find())
				{
					path = pathFinder.formatPath();
				}else
					wurst.chat.error("Could not find a path.");
				enabled = true;
				wurst.events.add(RenderListener.class, PathCmd.this);
				
				if(debugMode.isChecked())
					System.out.println("Length: " + path.size()
						+ ", processed: "
						+ pathFinder.getProcessedPoints().size() + ", queue: "
						+ pathFinder.getQueuedPoints().length + ", cost: "
						+ pathFinder.getRawPath().getTotalCost());
				System.out.println("Done after "
					+ (System.nanoTime() - startTime) / 1e6 + "ms");
			}
		});
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}
	
	@Override
	public void onRender()
	{
		// arrow
		class Arrow
		{
			int arrows;
			
			void render(BlockPos start, BlockPos end)
			{
				double x = start.getX() + 0.5
					- Minecraft.getMinecraft().getRenderManager().renderPosX;
				double y = start.getY() + 0.5
					- Minecraft.getMinecraft().getRenderManager().renderPosY;
				double z = start.getZ() + 0.5
					- Minecraft.getMinecraft().getRenderManager().renderPosZ;
				
				double nextX = end.getX() + 0.5
					- Minecraft.getMinecraft().getRenderManager().renderPosX;
				double nextY = end.getY() + 0.5
					- Minecraft.getMinecraft().getRenderManager().renderPosY;
				double nextZ = end.getZ() + 0.5
					- Minecraft.getMinecraft().getRenderManager().renderPosZ;
				
				glBegin(GL_LINES);
				{
					glVertex3d(x, y, z);
					glVertex3d(nextX, nextY, nextZ);
				}
				glEnd();
				
				glPushMatrix();
				glTranslated(nextX, nextY, nextZ);
				glScaled(1D / 16D, 1D / 16D, 1D / 16D);
				glRotated(Math.toDegrees(Math.atan2(nextY - y, z - nextZ)) + 90,
					1, 0, 0);
				glRotated(
					Math.toDegrees(Math.atan2(nextX - x,
						Math.sqrt(
							Math.pow(y - nextY, 2) + Math.pow(z - nextZ, 2)))),
					0, 0, 1);
				glBegin(GL_LINES);
				{
					glVertex3d(0, 2, 1);
					glVertex3d(-1, 2, 0);
					
					glVertex3d(-1, 2, 0);
					glVertex3d(0, 2, -1);
					
					glVertex3d(0, 2, -1);
					glVertex3d(1, 2, 0);
					
					glVertex3d(1, 2, 0);
					glVertex3d(0, 2, 1);
					
					glVertex3d(1, 2, 0);
					glVertex3d(-1, 2, 0);
					
					glVertex3d(0, 2, 1);
					glVertex3d(0, 2, -1);
					
					glVertex3d(0, 0, 0);
					glVertex3d(1, 2, 0);
					
					glVertex3d(0, 0, 0);
					glVertex3d(-1, 2, 0);
					
					glVertex3d(0, 0, 0);
					glVertex3d(0, 2, -1);
					
					glVertex3d(0, 0, 0);
					glVertex3d(0, 2, 1);
				}
				glEnd();
				glPopMatrix();
				
				arrows++;
			}
		}
		Arrow arrow = new Arrow();
		
		// GL settings
		glBlendFunc(770, 771);
		glEnable(GL_BLEND);
		glEnable(GL_LINE_SMOOTH);
		glDisable(GL11.GL_TEXTURE_2D);
		if(!depthTest.isChecked())
			glDisable(GL_DEPTH_TEST);
		glDisable(GL_CULL_FACE);
		glDepthMask(false);
		
		if(debugMode.isChecked())
		{
			// queue (yellow)
			glLineWidth(2.0F);
			glColor4f(1F, 1F, 0F, 0.75F);
			PathPoint[] queue = pathFinder.getQueuedPoints();
			for(int i = 0; i < queue.length; i++)
			{
				if(arrow.arrows >= 5000
					- pathFinder.getProcessedPoints().size())
					break;
				
				if(queue[i].getPrevious() == null)
					continue;
				
				BlockPos pos = queue[i].getPrevious().getPos();
				BlockPos nextPos = queue[i].getPos();
				
				arrow.render(pos, nextPos);
			}
			
			// processed (red)
			glLineWidth(2.0F);
			glColor4f(1F, 0F, 0F, 0.75F);
			for(PathPoint point : pathFinder.getProcessedPoints())
			{
				if(arrow.arrows >= 5000 - path.size())
					break;
				
				if(point.getPrevious() == null)
					continue;
				
				BlockPos pos = point.getPrevious().getPos();
				BlockPos nextPos = point.getPos();
				
				arrow.render(pos, nextPos);
			}
		}
		
		// path (blue)
		if(debugMode.isChecked())
		{
			glLineWidth(4.0F);
			glColor4f(0F, 0F, 1F, 0.75F);
		}else
		{
			glLineWidth(2.0F);
			glColor4f(0F, 1F, 0F, 0.75F);
		}
		for(int i = 0; i < path.size() - 1; i++)
		{
			BlockPos pos = path.get(i);
			BlockPos nextPos = path.get(i + 1);
			arrow.render(pos, nextPos);
		}
		
		// GL resets
		glEnable(GL11.GL_TEXTURE_2D);
		glEnable(GL_DEPTH_TEST);
		glDepthMask(true);
		glDisable(GL_BLEND);
	}
}
