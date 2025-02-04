package hardcorequesting.common.client.interfaces.graphic.task;

import com.mojang.blaze3d.vertex.PoseStack;
import hardcorequesting.common.client.EditMode;
import hardcorequesting.common.client.interfaces.GuiQuestBook;
import hardcorequesting.common.quests.Quest;
import hardcorequesting.common.quests.task.CompleteQuestTask;
import hardcorequesting.common.util.EditType;
import hardcorequesting.common.util.Positioned;
import hardcorequesting.common.util.SaveHelper;
import hardcorequesting.common.util.Translator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class CompleteQuestTaskGraphic extends ListTaskGraphic<CompleteQuestTask.Part> {
    private static final int Y_OFFSET = 30;
    private static final int X_TEXT_OFFSET = 23;
    private static final int X_TEXT_INDENT = 0;
    private static final int Y_TEXT_OFFSET = 0;
    private static final int ITEM_SIZE = 18;
    
    private final CompleteQuestTask task;
    
    public CompleteQuestTaskGraphic(CompleteQuestTask task, UUID playerId, GuiQuestBook gui) {
        super(task, task.getParts(), playerId, gui);
        this.task = task;
        addDetectButton(task);
    }
    
    @Override
    protected List<Positioned<CompleteQuestTask.Part>> positionParts(List<CompleteQuestTask.Part> parts) {
        List<Positioned<CompleteQuestTask.Part>> list = new ArrayList<>(parts.size());
        int x = START_X;
        int y = START_Y;
        for (CompleteQuestTask.Part part : parts) {
            list.add(new Positioned<>(x, y, part));
            y += Y_OFFSET;
        }
        return list;
    }
    
    @Override
    protected void drawPart(PoseStack matrices, CompleteQuestTask.Part part, int id, int x, int y, int mX, int mY) {
        part.getIconStack().ifLeft(itemStack -> gui.drawItemStack(matrices, itemStack, x, y, mX, mY, false))
                .ifRight(fluidStack -> gui.drawFluid(fluidStack, matrices, x, y, mX, mY));
        
        if (part.getQuest() != null) {
            gui.drawString(matrices, Translator.plain(part.getName()), x + X_TEXT_OFFSET, y + Y_TEXT_OFFSET, 0x404040);
            if (task.completed(id, playerId)) {
                gui.drawString(matrices, Translator.translatable("hqm.completedMenu.visited").withStyle(ChatFormatting.DARK_GREEN), x + X_TEXT_OFFSET + X_TEXT_INDENT, y + Y_TEXT_OFFSET + 9, 0.7F, 0x404040);
            }
        } else {
            gui.drawString(matrices, Translator.translatable("hqm.completionTask.firstline").withStyle(ChatFormatting.DARK_RED), x + X_TEXT_OFFSET, y + Y_TEXT_OFFSET, 0x404040);
            gui.drawString(matrices, Translator.translatable("hqm.completionTask.secondline").withStyle(ChatFormatting.DARK_RED), x + X_TEXT_OFFSET, y + Y_TEXT_OFFSET + 9, 0x404040);
            gui.drawString(matrices, Translator.translatable("hqm.completionTask.thirdline").withStyle(ChatFormatting.DARK_RED), x + X_TEXT_OFFSET, y + Y_TEXT_OFFSET + 18, 0x404040);
        }
    }
    
    @Override
    protected boolean handleEditPartClick(EditMode mode, CompleteQuestTask.Part part, int id) {
        if (super.handleEditPartClick(mode, part, id)) {
            return true;
        } else if (Quest.speciallySelectedQuestId != null) {
            parts.getOrCreateForModify(id).setQuest(Quest.speciallySelectedQuestId);
            SaveHelper.add(EditType.COMPLETE_CHECK_CHANGE);
            return true;
        }
        return false;
    }
    
    @Override
    protected boolean isInPartBounds(int mX, int mY, Positioned<CompleteQuestTask.Part> pos) {
        return gui.inBounds(pos.getX(), pos.getY(), ITEM_SIZE, ITEM_SIZE, mX, mY);
    }
}
