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
package com.golden.gamedev.engine;

// GTGE
import com.golden.gamedev.util.Utility;


/**
 * Audio manager that manages playing, stopping, looping of multiple audio
 * sounds (<code>BaseAudioRenderer</code>s). <p>
 *
 * Audio manager takes up a single base renderer parameter. The base is used to
 * create new instance of <code>BaseAudioRenderer</code> to play new audio
 * sound. <p>
 *
 * Audio manager also take care any idle renderer and looping audio renderer. <p>
 *
 * This class is using {@link BaseIO} to get the external resources.
 */
public class BaseAudio implements Runnable {


 /***************************** AUDIO POLICY *********************************/

	/**
	 * Audio clip with a same name only can be played once at a time. The audio
	 * clip is continued if the clip is currently playing. <p>
	 *
	 * To force the clip to replay, set the audio policy to
	 * {@link #SINGLE_REPLAY} instead.
	 *
	 * @see #setAudioPolicy(int)
	 * @see #play(String, int)
	 */
	public static final int SINGLE = 0;

	/**
	 * Multiple audio clips can be played at the same time (simultaneous). <br>
	 * Note: when using {@link #setExclusive(boolean) exclusive mode} (only
	 * <b>one</b> audio clip can be played at a time), <code>MULTIPLE</code>
	 * policy is obsolete, and automatically changed into {@link #SINGLE}.
	 *
	 * @see #setAudioPolicy(int)
	 * @see #play(String, int)
	 */
	public static final int MULTIPLE = 1;

	/**
	 * Same as {@link #SINGLE} policy except the audio clip is force to replay.
	 *
	 * @see #setAudioPolicy(int)
	 * @see #play(String, int)
	 */
	public static final int SINGLE_REPLAY = 2;


	private int audioPolicy = MULTIPLE;			// default audio policy

	private int maxSimultaneous;				// max simultaneous audio sound
												// played at a time

 /**************************** AUDIO RENDERER ********************************/

	private BaseAudioRenderer	baseRenderer;

	private BaseAudioRenderer[] renderer;

	private String[] 			rendererFile;	// store the filename of
												// the rendered audio

	private String				lastAudioFile;	// last played audio


 /*************************** MANAGER PROPERTIES *****************************/

    private BaseIO 		base;

	private boolean 	exclusive;	// only one clip can be played at a time

	private boolean		loop;		// ALL clips are played continously or not

    private float		volume;

    private boolean		active;

    private int			buffer;	    // total audio renderer instances before
									// attempting to replace idle renderer


 /****************************************************************************/
 /**************************** CONSTRUCTOR ***********************************/
 /****************************************************************************/

	/**
	 * Creates new audio manager with specified renderer as the base renderer
	 * of all audio sounds created by this audio manager. <p>
	 *
	 * @param base			the BaseIO to get audio resources
	 * @param baseRenderer	the base renderer of this audio manager
	 */
	public BaseAudio(BaseIO base, BaseAudioRenderer baseRenderer) {
		this.base = base;
		this.baseRenderer = baseRenderer;

		active = baseRenderer.isAvailable();
		volume = 1.0f;
		buffer = 10;
		maxSimultaneous = 6;

		renderer = new BaseAudioRenderer[0];
		rendererFile = new String[0];

		Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Thread implementation for managing audio renderers looping.
	 */
	public void run() {
		while (true) {
			try {
			    Thread.sleep(100L);
			} catch (InterruptedException e) { }

			for (int i=0;i < renderer.length;i++) {
				if (renderer[i].isLoop() &&
					renderer[i].getStatus() == BaseAudioRenderer.END_OF_SOUND) {
					renderer[i].play();
				}
			}
		}
	}


 /****************************************************************************/
 /************************ PLAYING AUDIO OPERATION ***************************/
 /****************************************************************************/

	/**
	 * Plays audio clip with {@link #getAudioPolicy() default policy}.
	 *
	 * @return 	Slot which the audio is played.
	 * @see		#getAudioRenderer(int)
	 */
	public int play(String audiofile) {
		return play(audiofile, audioPolicy);
	}

	/**
	 * Plays an audio clip based on specified policy
	 * ({@link #SINGLE}, {@link #MULTIPLE}, {@link #SINGLE_REPLAY}).
	 *
	 * @return 	Slot which the audio is played.
	 * @see		#getAudioRenderer(int)
	 */
	public int play(String audiofile, int policy) {
		lastAudioFile = audiofile;

		if (!active) {
			return -1;
		}

		// -2 means attempt to replace idle renderer
		// since total renderer has exceed buffer size
		int emptyslot = (renderer.length <= buffer) ? -1 : -2;

		// to count simultaneous playing sound
		int playedSound = 0;

		for (int i=0;i < renderer.length;i++) {
			if (rendererFile[i].equals(audiofile)) {
				if (renderer[i].getStatus() == BaseAudioRenderer.PLAYING) {
					playedSound++;
				}

				if (policy == MULTIPLE && !exclusive) {
					if (renderer[i].getStatus() != BaseAudioRenderer.PLAYING) {
						renderer[i].setVolume(volume);
					    renderer[i].play();
					    return i;
					}

				} else if (policy == SINGLE_REPLAY) {
					// replay the sound
					if (exclusive) {
						stopAll();
					} else {
						renderer[i].stop();
					}

					renderer[i].setVolume(volume);
					renderer[i].play();

					return i;

				} else {
					// single policy no replay OR
					// multiple policy and exclusive mode
					if (exclusive) {
						// stop all except this audio renderer
						stopAll(renderer[i]);
					}

					if (renderer[i].getStatus() != BaseAudioRenderer.PLAYING) {
						renderer[i].setVolume(volume);
						renderer[i].play();
					}

					return i;
				}
			}

			// replace this idle slot
			if (emptyslot == -2 &&
				renderer[i].getStatus() != BaseAudioRenderer.PLAYING) {
				emptyslot = i;
			}
		}

		//////// attempt to play sound in new slot ////////

		// check for simultaneous sounds
		if (playedSound >= maxSimultaneous) {
			// too many simultaneous sounds!
			return -1;
		}

		if (emptyslot < 0) {
			// no empty slot, expand the renderer array
			renderer = (BaseAudioRenderer[]) Utility.expand(renderer, 1);
			rendererFile = (String[]) Utility.expand(rendererFile, 1);
			emptyslot = renderer.length-1;
		}


		if (renderer[emptyslot] == null) {
			// create new renderer in the empty slot
			renderer[emptyslot] = createRenderer();
			renderer[emptyslot].setLoop(loop);
		}


		if (exclusive) {
			// in exclusive mode, only one clip can be played at a time
			stopAll();
		} else {
			// to be sure the renderer is not playing
			stop(emptyslot);
		}


		renderer[emptyslot].setVolume(volume);
		renderer[emptyslot].play(base.getURL(audiofile));
		rendererFile[emptyslot] = audiofile;

		return emptyslot;
	}


	/**
	 * Stops audio playback in specified slot.
	 */
	public void stop(int slot) {
		if (renderer[slot].getStatus() == BaseAudioRenderer.PLAYING) {
			renderer[slot].stop();
		}
	}

	/**
	 * Stops audio playback with specified name.
	 */
	public void stop(String audiofile) {
		BaseAudioRenderer audio = getAudioRenderer(audiofile);

		if (audio != null) {
			audio.stop();
		}
	}

	/**
	 * Stops all played audio playbacks in this audio manager.
	 */
	public void stopAll() {
		int count = renderer.length;
		for (int i=0;i < count;i++) {
			stop(i);
		}
	}

	/**
	 * Stops all played audio playbacks in this audio manager except specified
	 * renderer.
	 *
	 * @see #getAudioRenderer(String)
	 * @see #getAudioRenderer(int)
	 */
	public void stopAll(BaseAudioRenderer except) {
		int count = renderer.length;
		for (int i=0;i < count;i++) {
			if (renderer[i] != except) {
				stop(i);
			}
		}
	}


 /****************************************************************************/
 /*********************** LOADED RENDERER TRACKER ****************************/
 /****************************************************************************/

	/**
	 * Returns audio renderer in specified slot.
	 */
	public BaseAudioRenderer getAudioRenderer(int slot) {
		return renderer[slot];
	}

	/**
	 * Returns audio renderer with specified audio file or null if not found.
	 */
	public BaseAudioRenderer getAudioRenderer(String audiofile) {
		int count = renderer.length;
		for (int i=0;i < count;i++) {
			// find renderer with specified audio file
			if (rendererFile[i].equals(audiofile)) {
				return renderer[i];
			}
		}

		return null;
	}

	/**
	 * Returns the last played audio file. <p>
	 *
	 * This method is used for example when audio manager is set to active state
	 * from inactive state, if the game wish to play the last played audio, call
	 * {@link #play(String) play(getLastAudioFile())}.
	 *
	 * @see #play(String)
	 */
	public String getLastAudioFile() {
		return lastAudioFile;
	}

	/**
	 * Returns all audio renderers (playing and idle renderer) associated with
	 * this audio manager.
	 *
	 * @see #getCountRenderers()
	 */
	public BaseAudioRenderer[] getRenderers() {
		return renderer;
	}

	/**
	 * Returns total audio renderer created within this audio manager.
	 *
	 * @see #getRenderers()
	 */
	public int getCountRenderers() {
		return renderer.length;
	}


 /****************************************************************************/
 /********************** SETTINGS AUDIO VOLUME *******************************/
 /****************************************************************************/

	/**
	 * Returns audio manager volume.
	 *
	 * @see #setVolume(float)
	 */
	public float getVolume() {
		return volume;
	}

	/**
	 * Sets audio manager volume range in [0.0f - 1.0f]. <p>
	 *
	 * If setting volume of {@linkplain #getBaseRenderer() base renderer}
	 * is not supported, this method will return immediately.
	 *
	 * @see #getVolume()
	 */
	public void setVolume(float volume) {
		if (volume < 0.0f) volume = 0.0f;
		if (volume > 1.0f) volume = 1.0f;

		if (baseRenderer.isVolumeSupported() == false || this.volume == volume) {
			return;
		}

		this.volume = volume;

		int count = renderer.length;
		for (int i=0;i < count;i++) {
			renderer[i].setVolume(volume);
		}
	}

	/**
	 * Returns whether setting audio volume is supported or not.
	 */
	public boolean isVolumeSupported() {
		return baseRenderer.isVolumeSupported();
	}


 /****************************************************************************/
 /************************** MANAGER PROPERTIES ******************************/
 /****************************************************************************/

	/**
	 * Returns the default audio policy used by this audio manager to play audio
	 * sound when no audio policy is specified.
	 *
	 * @see #play(String)
	 */
	public int getAudioPolicy() {
		return audioPolicy;
	}

	/**
	 * Sets the default audio policy used by this audio manager to play audio
	 * sound when no audio policy is specified.
	 *
	 * @param i	the default audio policy, one of {@link #SINGLE},
	 * 			{@link #MULTIPLE}, {@link #SINGLE_REPLAY}
	 * @see #play(String)
	 */
	public void setAudioPolicy(int i) {
		audioPolicy = i;
	}

	/**
	 * Returns maximum simultaneous same audio sound can be played at a time.
	 */
	public int getMaxSimultaneous() {
		return maxSimultaneous;
	}

	/**
	 * Sets maximum simultaneous same audio sound can be played at a time.
	 */
	public void setMaxSimultaneous(int i) {
		maxSimultaneous = i;
	}

	/**
	 * Returns true, if only one clip is allowed to play at a time.
	 *
	 * @see #setExclusive(boolean)
	 */
	public boolean isExclusive() {
		return exclusive;
	}

	/**
	 * Sets whether only one clip is allowed to play at a time or not.
	 *
	 * @param exclusive 	true, only one clip is allowed to play at a time
	 * @see #isExclusive()
	 */
	public void setExclusive(boolean exclusive) {
		this.exclusive = exclusive;

		if (exclusive) {
			stopAll();
		}
	}

	/**
	 * Returns total renderer allowed to create before audio manager attempt to
	 * replace idle renderer.
	 *
	 * @see #setBuffer(int)
	 */
	public int getBuffer() {
		return buffer;
	}

	/**
	 * Sets total renderer allowed to create before audio manager attempt to
	 * replace idle renderer.
	 *
	 * @see #getBuffer()
	 */
	public void setBuffer(int i) {
		buffer = i;
	}

	/**
	 * Returns true, if all the audio sounds are played continously.
	 *
	 * @see #setLoop(boolean)
	 */
	public boolean isLoop() {
		return loop;
	}

	/**
	 * Sets whether all the audio sounds should be played continously or not.
	 *
	 * @see #isLoop()
	 */
	public void setLoop(boolean loop) {
		if (this.loop == loop) {
			return;
		}

		this.loop = loop;

		int count = renderer.length;
		for (int i=0;i < count;i++) {
			renderer[i].setLoop(loop);
		}
	}

	/**
	 * Returns <code>BaseIO</code> from where this audio manager is getting all
	 * audio sound resources.
	 *
	 * @see #setBaseIO(BaseIO)
	 */
	public BaseIO getBaseIO() {
		return base;
	}

	/**
	 * Sets <code>BaseIO</code> from where this audio manager is getting all
	 * audio sound resources.
	 *
	 * @see #getBaseIO()
	 */
	public void setBaseIO(BaseIO base) {
		this.base = base;
	}


	/**
	 * Returns true, if this audio manager is fully functional.
	 *
	 * @see #setActive(boolean)
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Turn on/off this audio manager. <p>
	 *
	 * Note: {@linkplain #isAvailable() unavailable} audio manager can't be
	 * switch to on.
	 *
	 * @param b true, turn on this audio manager
	 * @see #isActive()
	 * @see #isAvailable()
	 */
	public void setActive(boolean b) {
		active = (isAvailable()) ? b : false;

		if (!active) {
			stopAll();
		}
	}

	/**
	 * Returns whether this audio manager is available to use or not. <p>
	 *
	 * Unavailable audio manager is caused by
	 * {@link BaseAudioRenderer#isAvailable() unavailable}
	 * {@link #getBaseRenderer() base renderer}.
	 */
	public boolean isAvailable() {
		return baseRenderer.isAvailable();
	}


 /****************************************************************************/
 /***************************** BASE RENDERER ********************************/
 /****************************************************************************/

	/**
	 * Returns the base renderer of this audio manager.
	 *
	 * @see #setBaseRenderer(BaseAudioRenderer)
	 */
	public BaseAudioRenderer getBaseRenderer() {
		return baseRenderer;
	}

	/**
	 * Sets specified audio renderer as this audio manager base renderer. <p>
	 *
	 * All renderers in this audio manager is created based on this base
	 * renderer.
	 *
	 * @see #getBaseRenderer()
	 */
	public void setBaseRenderer(BaseAudioRenderer renderer) {
		baseRenderer = renderer;

		if (active) {
			active = baseRenderer.isAvailable();
		}
	}

	/**
	 * Constructs new audio renderer to play new audio sound. <p>
	 *
	 * The new audio renderer is created using <code>Class.forName(String)</code>
	 * from the {@linkplain #getBaseRenderer() base renderer} class name.
	 *
	 * @see #getBaseRenderer()
	 */
	protected BaseAudioRenderer createRenderer() {
		try {
			return (BaseAudioRenderer) Class.forName(baseRenderer.getClass().getName()).newInstance();
		} catch (Exception e) {
			throw new RuntimeException(
				"Unable to create new instance of audio renderer on " +
				this + " audio manager caused by: " + e.getMessage() + "\n" +
				"Make sure the base renderer has one empty constructor!");
		}
	}

}