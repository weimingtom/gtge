/*
 * Copyright (c) 2008 Golden T Studios.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.golden.gamedev;

// JFC
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import com.golden.gamedev.object.Background;
import com.golden.gamedev.object.GameFont;
import com.golden.gamedev.object.Sprite;
import com.golden.gamedev.util.Utility;

/**
 * Similar like <code>Game</code> class except this class is working under
 * <code>GameEngine</code> frame work.
 * <p>
 * 
 * <code>GameObject</code> class is plain same with <code>Game</code> class,
 * you can first create the game as <code>Game</code> class, run it, test it,
 * and then rename it to <code>GameObject</code> and attach it to
 * <code>GameEngine</code> frame work as one of game entities.
 * <p>
 * 
 * Please read {@link GameEngine} documentation for more information about how
 * to work with <code>GameObject</code> class.
 * 
 * @see com.golden.gamedev.GameEngine
 * @see com.golden.gamedev.Game
 */
public abstract class GameObject extends BaseGame {
	
	/** **************************** MASTER ENGINE ****************************** */
	
	/**
	 * The master <code>GameEngine</code> frame work.
	 */
	public final GameEngine parent;
	
	/** **************************** GAME ENGINE ******************************** */
	
	/** ************************* OTHER PROPERTIES ****************************** */
	
	private boolean finish; // true, to back to game chooser
	private boolean initialized; // true, indicates the game has been
	
	// initialized
	
	// to avoid double initialization
	// if the game is replaying
	
	/** ************************************************************************* */
	/** ************************* CONSTRUCTOR *********************************** */
	/** ************************************************************************* */
	
	/**
	 * Creates new <code>GameObject</code> with specified
	 * <code>GameEngine</code> as the master engine.
	 */
	public GameObject(GameEngine parent) {
		this.parent = parent;
		
		this.grabEngines();
	}
	
	private void grabEngines() {
		this.bsGraphics = this.parent.bsGraphics;
		this.bsIO = this.parent.bsIO;
		this.bsLoader = this.parent.bsLoader;
		this.bsInput = this.parent.bsInput;
		this.bsTimer = this.parent.bsTimer;
		this.bsMusic = this.parent.bsMusic;
		this.bsSound = this.parent.bsSound;
		
		this.fontManager = this.parent.fontManager;
	}
	
	/**
	 * Starts the game main loop, this method will not return until the game is
	 * finished playing/running. To end the game call {@linkplain #finish()}
	 * method.
	 */
	public final void start() {
		// grabbing engines from master engine
		this.grabEngines();
		GameFont fpsFont = this.parent.fpsFont;
		if (!this.initialized) {
			this.initResources();
			this.initialized = true;
		}
		
		this.finish = false;
		
		// start game loop!
		// before play, clear memory (runs garbage collector)
		System.gc();
		System.runFinalization();
		
		this.bsInput.refresh();
		this.bsTimer.refresh();
		
		long elapsedTime = 0;
		out: while (true) {
			if (this.parent.inFocus) {
				// update game
				this.update(elapsedTime);
				this.parent.update(elapsedTime); // update common variables
				this.bsInput.update(elapsedTime);
				
			}
			else {
				// the game is not in focus!
				try {
					Thread.sleep(300);
				}
				catch (InterruptedException e) {
				}
			}
			
			do {
				if (this.finish || !this.parent.isRunning()) {
					// if finish, quit this game
					break out;
				}
				
				// graphics operation
				Graphics2D g = this.bsGraphics.getBackBuffer();
				
				this.render(g); // render game
				this.parent.render(g); // render global game
				
				if (!this.parent.isDistribute()) {
					// if the game is still under development
					// draw game FPS and other stuff
					
					// to make sure the FPS is drawn!
					// remove any clipping and alpha composite
					if (g.getClip() != null) {
						g.setClip(null);
					}
					if (g.getComposite() != null) {
						if (g.getComposite() != AlphaComposite.SrcOver) {
							g.setComposite(AlphaComposite.SrcOver);
						}
					}
					
					fpsFont.drawString(g, "FPS = " + this.getCurrentFPS() + "/"
					        + this.getFPS(), 9, this.getHeight() - 21);
					
					fpsFont.drawString(g, "GTGE", this.getWidth() - 65, 9);
				}
				
				if (!this.parent.inFocus) {
					this.parent.renderLostFocus(g);
				}
				
			} while (this.bsGraphics.flip() == false);
			
			elapsedTime = this.bsTimer.sleep();
			
			if (elapsedTime > 100) {
				// can't lower than 10 fps (1000/100)
				elapsedTime = 100;
			}
		}
	}
	
	/**
	 * End this game, and back to
	 * {@linkplain GameEngine#getGame(int) game object chooser}.
	 * 
	 * @see GameEngine#nextGameID
	 * @see GameEngine#nextGame
	 */
	public void finish() {
		this.finish = true;
	}
	
	/** ************************************************************************* */
	/** ***************************** MAIN METHODS ****************************** */
	/** ************************************************************************* */
	
	/**
	 * All game resources initialization, everything that usually goes to
	 * constructor should be put in here.
	 * <p>
	 * 
	 * This method is called only once for every newly created
	 * <code>GameObject</code> class.
	 * 
	 * @see #getImage(String)
	 * @see #getImages(String, int, int)
	 * @see #playMusic(String)
	 * @see #setMaskColor(Color)
	 * @see com.golden.gamedev.object
	 */
	public abstract void initResources();
	
	/** ************************************************************************* */
	/** *********************** ESSENTIAL GAME UTILITY ************************** */
	/** ************************************************************************* */
	// -> com.golden.gamedev.util.Utility
	/**
	 * Effectively equivalent to the call
	 * {@linkplain com.golden.gamedev.util.Utility#getRandom(int, int)
	 * Utility.getRandom(int, int)}
	 */
	public int getRandom(int low, int hi) {
		return Utility.getRandom(low, hi);
	}
	
	// INTERNATIONALIZATION UTILITY
	// public Locale getLocale() { return locale; }
	// public void setLocale(Locale locale) { this.locale = locale; }
	
	/** ************************************************************************* */
	/** ************************** AUDIO UTILITY ******************************** */
	/** ************************************************************************* */
	// -> com.golden.gamedev.engine.BaseAudio
	/**
	 * Effectively equivalent to the call
	 * {@linkplain com.golden.gamedev.engine.BaseAudio#play(String)
	 * bsMusic.play(String)}.
	 * 
	 * @see com.golden.gamedev.engine.BaseAudio#setBaseRenderer(com.golden.gamedev.engine.BaseAudioRenderer)
	 * @see com.golden.gamedev.engine.audio
	 */
	public int playMusic(String audiofile) {
		return this.bsMusic.play(audiofile);
	}
	
	/**
	 * Effectively equivalent to the call
	 * {@linkplain com.golden.gamedev.engine.BaseAudio#play(String)
	 * bsSound.play(String)}.
	 * 
	 * @see com.golden.gamedev.engine.BaseAudio#setBaseRenderer(com.golden.gamedev.engine.BaseAudioRenderer)
	 * @see com.golden.gamedev.engine.audio
	 */
	public int playSound(String audiofile) {
		return this.bsSound.play(audiofile);
	}
	
	/**
	 * Effectively equivalent to the call
	 * {@linkplain com.golden.gamedev.engine.BaseTimer#getCurrentFPS()
	 * bsTimer.getCurrentFPS()}.
	 */
	public int getCurrentFPS() {
		return this.bsTimer.getCurrentFPS();
	}
	
	/**
	 * Effectively equivalent to the call
	 * {@linkplain com.golden.gamedev.engine.BaseTimer#getFPS()}.
	 */
	public int getFPS() {
		return this.bsTimer.getFPS();
	}
	
	/**
	 * Draws game frame-per-second (FPS) to specified location.
	 */
	public void drawFPS(Graphics2D g, int x, int y) {
		this.fontManager.getFont("FPS Font").drawString(g,
		        "FPS = " + this.getCurrentFPS() + "/" + this.getFPS(), x, y);
	}
	
	/** ************************************************************************* */
	/** ************************** INPUT UTILITY ******************************** */
	/** ************************************************************************* */
	// -> com.golden.gamedev.engine.BaseInput
	/**
	 * Effectively equivalent to the call
	 * {@linkplain com.golden.gamedev.engine.BaseInput#getMouseX()
	 * bsInput.getMouseX()}.
	 */
	public int getMouseX() {
		return this.bsInput.getMouseX();
	}
	
	/**
	 * Effectively equivalent to the call
	 * {@linkplain com.golden.gamedev.engine.BaseInput#getMouseY()
	 * bsInput.getMouseY()}.
	 */
	public int getMouseY() {
		return this.bsInput.getMouseY();
	}
	
	/**
	 * Returns whether the mouse pointer is inside specified screen boundary.
	 */
	public boolean checkPosMouse(int x1, int y1, int x2, int y2) {
		return (this.getMouseX() >= x1 && this.getMouseY() >= y1
		        && this.getMouseX() <= x2 && this.getMouseY() <= y2);
	}
	
	/**
	 * Returns whether the mouse pointer is inside specified sprite boundary.
	 * 
	 * @param sprite sprite to check its intersection with mouse pointer
	 * @param pixelCheck true, checking the sprite image with pixel precision
	 */
	public boolean checkPosMouse(Sprite sprite, boolean pixelCheck) {
		Background bg = sprite.getBackground();
		
		// check whether the mouse is in background clip area
		if (this.getMouseX() < bg.getClip().x
		        || this.getMouseY() < bg.getClip().y
		        || this.getMouseX() > bg.getClip().x + bg.getClip().width
		        || this.getMouseY() > bg.getClip().y + bg.getClip().height) {
			return false;
		}
		
		double mosx = this.getMouseX() + bg.getX() - bg.getClip().x;
		double mosy = this.getMouseY() + bg.getY() - bg.getClip().y;
		
		if (pixelCheck) {
			try {
				return ((sprite.getImage().getRGB((int) (mosx - sprite.getX()),
				        (int) (mosy - sprite.getY())) & 0xFF000000) != 0x00);
			}
			catch (Exception e) {
				return false;
			}
			
		}
		else {
			return (mosx >= sprite.getX() && mosy >= sprite.getY()
			        && mosx <= sprite.getX() + sprite.getWidth() && mosy <= sprite
			        .getY()
			        + sprite.getHeight());
		}
	}
	
	/**
	 * Effectively equivalent to the call
	 * {@linkplain com.golden.gamedev.engine.BaseInput#isMousePressed(int)
	 * bsInput.isMousePressed(java.awt.event.MouseEvent.BUTTON1)}.
	 */
	public boolean click() {
		return this.bsInput.isMousePressed(MouseEvent.BUTTON1);
	}
	
	/**
	 * Effectively equivalent to the call
	 * {@linkplain com.golden.gamedev.engine.BaseInput#setMouseVisible(boolean)
	 * bsInput.setMouseVisible(false)}.
	 */
	public void hideCursor() {
		this.bsInput.setMouseVisible(false);
	}
	
}