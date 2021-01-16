package su.nexmedia.engine.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import su.nexmedia.engine.core.Version;
import su.nexmedia.engine.utils.constants.JStrings;

/**
 * @author Fogus® Multimedia 
 */
public class ClickText {

	private String text;
	private ComponentBuilder builder;
	private Map<String, ClickWord> replacers;
	
	public ClickText(@NotNull String text) {
		this.text = StringUT.color(text);
		this.builder = new ComponentBuilder("");
		this.replacers = new HashMap<>();
	}
	
	@NotNull
	public ClickWord createFullPlaceholder() {
		ClickWord clickWord = new ClickWord(this.text);
		this.replacers.put(JStrings.MASK_ANY, clickWord);
		return clickWord;
	}
	
	@NotNull
	public ClickWord createPlaceholder(@NotNull String placeholder, @NotNull String text) {
		ClickWord clickWord = new ClickWord(text);
		this.replacers.put(placeholder, clickWord);
		return clickWord;
	}
	
	@NotNull
	private BaseComponent[] build(@NotNull String line) {
		this.builder = new ComponentBuilder("");
		
		// Support for full line the single ClickText
		ClickWord full = this.replacers.get(JStrings.MASK_ANY);
		if (full != null) {
			this.builder.append(full.build());//.event(full.getClickEvent()).event(full.getHoverEvent());
			return this.builder.create();
		}
		
		String[] words = line.split(" ");
		for (String word : words) {
			
			// Get text Json data if it present.
			Optional<String> optWord = this.replacers.keySet().stream()
					.filter(holder -> word.contains(holder)).findFirst();
			ClickWord clickWord = optWord.isPresent() ? this.replacers.get(optWord.get()) : null;
			
			if (clickWord != null) {
				this.builder.append(clickWord.build());
			}
			// FIXME: Saves bold, underline, etc. formatting for the next words even with &r.
			// Probably needs to be reworked for 1.16+
			else {
				this.builder.append(TextComponent.fromLegacyText(word), ComponentBuilder.FormatRetention.FORMATTING);
			}
			this.builder.append(TextComponent.fromLegacyText(" "), ComponentBuilder.FormatRetention.FORMATTING);
		}
		return this.builder.create();
	}
	
	public void send(@NotNull CommandSender sender) {
		if (sender instanceof Player) {
			for (String line : this.text.split("\n")) {
				((Player)sender).spigot().sendMessage(this.build(line));
			}
		}
	}
	
	public void send(@NotNull Set<Player> list) {
		for (String line : this.text.split("\n")) {
			BaseComponent[] jsonText = this.build(line);
			for (Player p : list) {
				p.spigot().sendMessage(jsonText);
			}
		}
	}
	
	public class ClickWord {
		
		private String text;
		public HoverEvent hover;
		public ClickEvent click;
		
		public ClickWord(@NotNull String text) {
			this.text = StringUT.color(text);
		}
		
		@NotNull
		public String getText() {
			return this.text;
		}
		
		@NotNull
		public HoverEvent getHoverEvent() {
			return this.hover;
		}
		
		@NotNull
		public ClickEvent getClickEvent() {
			return this.click;
		}
		
		@NotNull
		public ClickWord hint(@NotNull String text) {
			return this.hint(text.split("\n"));
		}
		
		@NotNull
		public ClickWord hint(@NotNull List<String> text) {
			return this.hint(text.toArray(new String[text.size()]));
		}
		
		@SuppressWarnings("deprecation")
		@NotNull
		public ClickWord hint(@NotNull String... text) {
			StringBuilder textBuilder = new StringBuilder();
			for (String line : text) {
				if (textBuilder.length() > 0) {
					textBuilder.append("\n");
				}
				textBuilder.append(line);
			}
			
			BaseComponent[] base = TextComponent.fromLegacyText(StringUT.color(textBuilder.toString()));
			if (Version.CURRENT.isHigher(Version.V1_15_R1)) {
				this.hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(base));
			}
			else {
				this.hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, base);
			}
			return this;
		}
		
		@SuppressWarnings("deprecation")
		@NotNull
		public ClickWord showItem(@NotNull ItemStack item) {
			//if (Version.CURRENT.isHigher(Version.V1_15_R1)) {
			//	Item item2 = new Item("f_item", item.getAmount(), ItemTag.ofNbt(FogusCore.get().getNMS().getNbtString(item)));
			//	this.hover = new HoverEvent(HoverEvent.Action.SHOW_ITEM, item2);
			//}
			//else {
				this.hover = new HoverEvent(HoverEvent.Action.SHOW_ITEM, toBase(item));
			//}
			return this;
		}
		
		@Deprecated
		public void showEntity(@NotNull String json) {
			this.hover = new HoverEvent(HoverEvent.Action.SHOW_ENTITY, toBase(json));
		}
		
		@Deprecated
		public void achievement(@NotNull String id) {
			this.hover = new HoverEvent(HoverEvent.Action.SHOW_ACHIEVEMENT, toBase(StringUT.colorOff(id)));
		}
		
		//
		
		@NotNull
		public ClickWord execCmd(@NotNull String cmd) {
			this.click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd);
			return this;
		}
		
		@NotNull
		public ClickWord suggCmd(@NotNull String cmd) {
			this.click = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, cmd);
			return this;
		}
		
		@NotNull
		public ClickWord url(@NotNull String url) {
			this.click = new ClickEvent(ClickEvent.Action.OPEN_URL, url);
			return this;
		}
		
		@NotNull
		public ClickWord file(@NotNull String path) {
			this.click = new ClickEvent(ClickEvent.Action.OPEN_FILE, path);
			return this;
		}
		
		@Deprecated
		public void page(int page) {
			this.click = new ClickEvent(ClickEvent.Action.CHANGE_PAGE, String.valueOf(page));
		}
		
		//
		
		@NotNull
		public TextComponent build() {
			TextComponent component = new TextComponent(TextComponent.fromLegacyText(this.text));
			if (this.hover != null) {
				component.setHoverEvent(this.hover);
			}
			if (this.click != null) {
				component.setClickEvent(this.click);
			}
			return component;
		}
		
		@NotNull
		@Deprecated
		private BaseComponent[] toBase(@NotNull String text) {
			return new BaseComponent[] {new TextComponent(text)};
	    }
		
		@NotNull
		private BaseComponent[] toBase(@NotNull ItemStack item) {
			String json = ItemUT.toJson(item);
			return TextComponent.fromLegacyText(json);
	    }
	}
}
