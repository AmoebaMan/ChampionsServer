package net.amoebaman.championsserver.utils;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;

public class ChatUtils {

	/** The default Minecraft chat box width, in font-pixels */
	private static final int SCREEN_WIDTH = 316;
	
	/** Most characters in Minecraft's default font are this many font-pixels wide */
	private static final int DEFAULT_CHAR_WIDTH = 6;
	
	/** A map of the width of all irregular characters in Minecraft's default font */
	private static final HashMap<Character, Integer> IRREG_CHAR_WIDTH = new HashMap<Character, Integer>();
	static{
		IRREG_CHAR_WIDTH.put(' ', 4);
		IRREG_CHAR_WIDTH.put('i', 2);
		IRREG_CHAR_WIDTH.put('I', 4);
		IRREG_CHAR_WIDTH.put('k', 5);
		IRREG_CHAR_WIDTH.put('l', 3);
		IRREG_CHAR_WIDTH.put('t', 4);
		IRREG_CHAR_WIDTH.put('!', 2);
		IRREG_CHAR_WIDTH.put('(', 5);
		IRREG_CHAR_WIDTH.put(')', 5);
		IRREG_CHAR_WIDTH.put('~', 7);
		IRREG_CHAR_WIDTH.put(',', 2);
		IRREG_CHAR_WIDTH.put('.', 2);
		IRREG_CHAR_WIDTH.put('<', 5);
		IRREG_CHAR_WIDTH.put('>', 5);
		IRREG_CHAR_WIDTH.put(':', 2);
		IRREG_CHAR_WIDTH.put(';', 2);
		IRREG_CHAR_WIDTH.put('"', 5);
		IRREG_CHAR_WIDTH.put('[', 4);
		IRREG_CHAR_WIDTH.put(']', 4);
		IRREG_CHAR_WIDTH.put('{', 5);
		IRREG_CHAR_WIDTH.put('}', 5);
		IRREG_CHAR_WIDTH.put('|', 2);
		IRREG_CHAR_WIDTH.put('`', 0);
		IRREG_CHAR_WIDTH.put('\'', 2);
		IRREG_CHAR_WIDTH.put(ChatColor.COLOR_CHAR, 0);
	}
	
	/**
	 * Gets the width of a character in Minecraft's default font, in font-pixels.
	 * @param value a character
	 * @return the width of the character
	 */
	private static int getCharWidth(char value){
		if(IRREG_CHAR_WIDTH.containsKey(value))
			return IRREG_CHAR_WIDTH.get(value);
		return DEFAULT_CHAR_WIDTH;
	}
	
	/**
	 * Gets the total width of some text in font-pixels, the sum of its characters.
	 * @param str some text
	 * @return the width of the text
	 */
	private static int getStringWidth(String str){
		int length = 0;
		for(int i = 0; i < str.length(); i++)
			if(i == 0)
				length += getCharWidth(str.charAt(i));
			else if(str.charAt(i - 1) != ChatColor.COLOR_CHAR)
				length += getCharWidth(str.charAt(i));
		return length;
	}
	
	/**
	 * Gets a line that when broadcast in chat will result in an empty line, since empty messages don't display at all.
	 * @return a spacer line
	 */
	public static String spacerLine(){
		return ChatColor.RESET.toString();
	}
	
	/**
	 * Gets a line of text, repeating the given pattern, that will fill one line of the chat box as completely as
	 * possible without spilling to the next line.
	 * @param pattern a pattern
	 * @return a filler line of the pattern repeated
	 */
	public static String fillerLine(String pattern){
		float length = getStringWidth(pattern);
		int iterations = (int) (SCREEN_WIDTH / length);
		String line = "";
		for(int i = 0; i < iterations; i++)
			line += pattern;
		return centerAlign(line);
	}
	
	/**
	 * Adds spaces the the beginning of a line of text to make it display as closely as possible to the center of chat box.
	 * @param text some text
	 * @return the formatted text for center-align
	 */
	public static String centerAlign(String text){
		int numSpaces = ((SCREEN_WIDTH - getStringWidth(text)) / 2) / getCharWidth(' ');
		for(int i = 0; i < numSpaces; i++)
			text = " " + text;
		return text;
	}
	
	/**
	 * Gets the closest possible interpretation of a dye color as a chat color, for Minecraft defaults.
	 * @param color a dye color
	 * @return the corresponding chat color
	 */
	public static ChatColor dyeToChatColor(DyeColor color){
		if(color == null) return ChatColor.RESET;
		switch(color){
		case BLACK: return ChatColor.BLACK;
		case BLUE: return ChatColor.BLUE;
		case BROWN: return ChatColor.BOLD;
		case CYAN: return ChatColor.DARK_AQUA;
		case GRAY: return ChatColor.DARK_GRAY;
		case GREEN: return ChatColor.DARK_GREEN;
		case LIGHT_BLUE: return ChatColor.BLUE;
		case LIME: return ChatColor.GREEN;
		case MAGENTA: return ChatColor.LIGHT_PURPLE;
		case ORANGE: return ChatColor.GOLD;
		case PINK: return ChatColor.RED;
		case PURPLE: return ChatColor.DARK_PURPLE;
		case RED: return ChatColor.RED;
		case SILVER: return ChatColor.GRAY;
		case WHITE: return ChatColor.WHITE;
		case YELLOW: return ChatColor.YELLOW;
		default: return ChatColor.RESET;
		}
	}
	
	/**
	 * Applies some basic formatting rules to the given text.  "&&" is replaced with the Minecraft
	 * color delimiter, "//" begins italic text (unless preceded by ':' as in a URL), "**" begins
	 * boldface text, "__" begins underlined text, and "\\" reverts to normal text.
	 * @param line some plain text
	 * @return the formatted text
	 */
	public static String format(String line){
		line = line.replace("[^~]&&", ChatColor.COLOR_CHAR + "");
		line = line.replaceAll("[^:]//", ChatColor.ITALIC.toString());
		line = line.replace("**", ChatColor.BOLD.toString());
		line = line.replace("__", ChatColor.UNDERLINE.toString());
		line = line.replace("\\\\", ChatColor.RESET.toString());
		return line;
	}
	
	/**
	 * Applies color-scheme specific formatting to the given text, in addition to standard formatting as
	 * specified by {@link ChatUtils#format(String)}.  The entire string is started off by the color
	 * scheme's default color.  "[[" begins the scheme's emphasis color, and "]]" reverts to the scheme's
	 * standard color.
	 * @param line some plain text
	 * @param scheme a color scheme
	 * @return the formatted text
	 */
	public static String format(String line, ColorScheme scheme){
		line = scheme.standard + format(line);
		line = line.replace("[[[", "[[¦");
		line = line.replace("]]]", "¥]]");
		line = line.replace("[[", scheme.emphasis.toString());
		line = line.replace("]]", scheme.standard.toString());
		line = line.replace("¦", "[");
		line = line.replace("¥", "]");
		return line;
	}
	
	/**
	 * Makes a progress bar, capped by square brackets and filled with colored pipes.  If the number of
	 * listed colors exceeds the number of listed values, and the listed values do not add up to the total
	 * value, the first unmatched color will be used to complete the bar.
	 * @param length the number of colored pipes that will be used in the bar
	 * @param total the total value of all values
	 * @param colors a list of colors to be used
	 * @param values a list of values corresponding with colors
	 * @return the progress bar, ready for display
	 */
	public static String makeProgressBar(int length, int total, List<ChatColor> colors, List<Integer> values){
		if(colors.size() < values.size())
			return ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "ERROR" + ChatColor.DARK_GRAY + "]";
		String bar = ChatColor.DARK_GRAY + "[";
		int pipes = 0;
		for(int i = 0; i < values.size(); i++){
			bar += colors.get(i);
			for(int j = 0; j < (1f * values.get(i) / total) * length; j++){
				bar += "|";
				pipes++;
			}
		}
		if(pipes < length && colors.size() > values.size()){
			bar += colors.get(values.size());
			for(int i = pipes; i < length; i++)
				bar += "|";
		}
		bar += ChatColor.DARK_GRAY + "]";
		return bar;
	}
	
	/**
	 * Broadcasts a set of centered, basic formatted lines, bordered on top and bottom by spacer lines.
	 * @param lines some text
	 */
	public static void bigBroadcast(String... lines){
		Bukkit.broadcastMessage(spacerLine());
		for(String str : lines)
			Bukkit.broadcastMessage(centerAlign(format(str)));
		Bukkit.broadcastMessage(spacerLine());
	}

	
	/**
	 * Broadcasts a set of centered, color formatted lines, bordered on top and bottom by spacer lines.
	 * @param scheme 
	 * @param lines some text
	 */
	public static void bigBroadcast(ColorScheme scheme, String... lines){
		Bukkit.broadcastMessage(spacerLine());
		for(String str : lines)
			Bukkit.broadcastMessage(centerAlign(format(str, scheme)));
		Bukkit.broadcastMessage(spacerLine());
	}
	
	public enum ColorScheme{
		NORMAL(ChatColor.GRAY, ChatColor.DARK_GRAY),
		HIGHLIGHT(ChatColor.GOLD, ChatColor.DARK_RED),
		ERROR(ChatColor.RED, ChatColor.DARK_GRAY),
		;
		public ChatColor standard, emphasis;
		private ColorScheme(ChatColor standard, ChatColor emphasis){
			this.standard = standard;
			this.emphasis = emphasis;
		}
	}
}
