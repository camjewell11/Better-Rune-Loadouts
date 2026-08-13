package com.camjewell.betterruneloadouts;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;
import net.runelite.api.Client;
import net.runelite.api.ScriptEvent;
import net.runelite.api.ScriptID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.game.chatbox.ChatboxInput;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.util.ColorUtil;

/**
 * A searchable grid of {@link RunePouchLoadoutIcon}s rendered inside the
 * chatbox, since there's no native rune pouch UI to repurpose for picking a
 * theme icon.
 */
class RunePouchLoadoutIconPicker extends ChatboxInput
{
	private final Client client;
	private final ChatboxPanelManager chatboxPanelManager;
	private final Consumer<Integer> onPick;

	private int currentSpriteId;
	private int scrollY;

	RunePouchLoadoutIconPicker(Client client, ChatboxPanelManager chatboxPanelManager, int currentSpriteId, Consumer<Integer> onPick)
	{
		this.client = client;
		this.chatboxPanelManager = chatboxPanelManager;
		this.currentSpriteId = currentSpriteId;
		this.onPick = onPick;
	}

	void show()
	{
		chatboxPanelManager.openInput(this);
	}

	@Override
	protected void open()
	{
		Widget container = chatboxPanelManager.getContainerWidget();
		container.deleteAllChildren();

		String prompt = ColorUtil.wrapWithColorTag("Search:", Color.BLACK);

		client.setVarcIntValue(VarClientID.MESLAYERMODE, 14);
		client.runScript(ScriptID.CHAT_TEXT_INPUT_REBUILD, prompt);

		Widget text = client.getWidget(InterfaceID.Chatbox.MES_TEXT2);
		text.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		text.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		text.setOriginalX(0);
		text.setOriginalY(0);
		text.setWidthMode(WidgetSizeMode.MINUS);
		text.setOriginalWidth(20);
		text.setHidden(false);
		text.setHasListener(true);
		text.setOnKeyListener((JavaScriptCallback) (ScriptEvent event) ->
		{
			client.runScript(112, event.getTypedKeyCode(), event.getTypedKeyChar(), prompt);
			update(client.getVarcStrValue(VarClientID.MESLAYERINPUT));
		});
		text.revalidate();

		Widget closeIcon = client.getWidget(InterfaceID.Chatbox.CLOSE_ICON);
		if (closeIcon != null)
		{
			closeIcon.setHasListener(true);
			closeIcon.setOnOpListener((JavaScriptCallback) (ScriptEvent event) -> chatboxPanelManager.close());
			closeIcon.revalidate();
		}

		update("");
	}

	private void update(String searchText)
	{
		Widget scrollArea = client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLAREA);
		scrollArea.setHidden(false);
		scrollArea.revalidate();

		List<RunePouchLoadoutIcon> icons = RunePouchLoadoutIcon.search(searchText);

		int columns = RunePouchGridConst.ICON_PICKER_COLUMNS;
		int iconSize = RunePouchGridConst.ICON_PICKER_ICON_SIZE;
		int spacing = RunePouchGridConst.ICON_PICKER_ICON_SPACING;

		int totalRows = icons.size() / columns + 1;
		int scrollHeight = Math.max(0, totalRows * (iconSize + spacing) + spacing);

		Widget scrollContents = client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS);
		scrollContents.deleteAllChildren();
		scrollContents.setScrollHeight(scrollHeight);
		scrollContents.setScrollY(scrollY);
		scrollContents.revalidate();

		// Wires the native vertical-scrollbar cs2 proc to our content pane.
		// See https://github.com/runelite/cs2-scripts/blob/master/scripts/%5Bproc%2Cscript7605%5D.cs2
		client.runScript(7605, InterfaceID.Chatbox.MES_LAYER_SCROLLBAR, InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS);

		for (int i = 0; i < icons.size(); i++)
		{
			RunePouchLoadoutIcon icon = icons.get(i);
			int row = i / columns;
			int col = i % columns;
			int x = col * (iconSize + spacing) + spacing;
			int y = row * (iconSize + spacing) + spacing;

			Widget highlight = scrollContents.createChild(-1, WidgetType.RECTANGLE);
			highlight.setFilled(true);
			highlight.setTextColor(0xFFFFFF);
			highlight.setWidthMode(WidgetSizeMode.ABSOLUTE);
			highlight.setHeightMode(WidgetSizeMode.ABSOLUTE);
			highlight.setOriginalWidth(iconSize + 4);
			highlight.setOriginalHeight(iconSize + 4);
			highlight.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			highlight.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			highlight.setOriginalX(x - 2);
			highlight.setOriginalY(y - 2);
			highlight.setOpacity(icon.spriteId == currentSpriteId ? 150 : 255);
			highlight.setHasListener(true);
			highlight.setAction(0, icon.name);
			highlight.setOnOpListener((JavaScriptCallback) (ScriptEvent event) ->
			{
				onPick.accept(icon.spriteId);
				chatboxPanelManager.close();
			});
			highlight.setOnMouseRepeatListener((JavaScriptCallback) (ScriptEvent event) ->
			{
				highlight.setOpacity(150);
				highlight.revalidate();
			});
			highlight.setOnMouseLeaveListener((JavaScriptCallback) (ScriptEvent event) ->
			{
				highlight.setOpacity(icon.spriteId == currentSpriteId ? 150 : 255);
				highlight.revalidate();
			});
			highlight.revalidate();

			Widget iconWidget = scrollContents.createChild(-1, WidgetType.GRAPHIC);
			iconWidget.setSpriteId(icon.spriteId);
			iconWidget.setWidthMode(WidgetSizeMode.ABSOLUTE);
			iconWidget.setHeightMode(WidgetSizeMode.ABSOLUTE);
			iconWidget.setOriginalWidth(RunePouchGridConst.CUSTOM_ICON_SIZE);
			iconWidget.setOriginalHeight(RunePouchGridConst.CUSTOM_ICON_SIZE);
			iconWidget.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			iconWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			iconWidget.setOriginalX(x);
			iconWidget.setOriginalY(y);
			iconWidget.revalidate();
		}
	}
}
