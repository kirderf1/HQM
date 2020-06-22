package hardcorequesting.client.interfaces.edit;

import hardcorequesting.client.interfaces.GuiBase;
import hardcorequesting.client.interfaces.GuiQuestBook;
import hardcorequesting.client.interfaces.LargeButton;
import hardcorequesting.client.interfaces.TextBoxGroup;
import hardcorequesting.quests.task.QuestTaskLocation;
import hardcorequesting.util.Translator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;

public class GuiEditMenuLocation extends GuiEditMenuExtended {
    
    private int id;
    private QuestTaskLocation task;
    private QuestTaskLocation.Location location;
    private PlayerEntity player;
    
    
    public GuiEditMenuLocation(GuiQuestBook gui, QuestTaskLocation task, final QuestTaskLocation.Location location, int id, PlayerEntity player) {
        super(gui, player, true, 180, 30, 20, 30);
        this.id = id;
        this.task = task;
        this.location = location;
        this.player = player;
        
        
        textBoxes.add(new TextBoxNumberNegative(gui, 0, "hqm.locationMenu.xTarget") {
            @Override
            protected void setValue(int number) {
                location.setX(number);
            }
            
            @Override
            protected int getValue() {
                return location.getX();
            }
        });
        
        textBoxes.add(new TextBoxNumberNegative(gui, 1, "hqm.locationMenu.yTarget") {
            @Override
            protected void setValue(int number) {
                location.setY(number);
            }
            
            @Override
            protected int getValue() {
                return location.getY();
            }
        });
        
        textBoxes.add(new TextBoxNumberNegative(gui, 2, "hqm.locationMenu.zTarget") {
            @Override
            protected void setValue(int number) {
                location.setZ(number);
            }
            
            @Override
            protected int getValue() {
                return location.getZ();
            }
        });
        
        // TODO Add Identifier textbox
//        textBoxes.add(new TextBoxNumberNegative(gui, 3, "hqm.locationMenu.dim") {
//            @Override
//            protected void setValue(int number) {
//                location.setDimension(number);
//            }
//            
//            @Override
//            protected int getValue() {
//                return location.getDimension();
//            }
//        });
        
        textBoxes.add(new TextBoxNumberNegative(gui, 4, "hqm.locationMenu.radius") {
            @Override
            protected int getValue() {
                return location.getRadius();
            }
            
            @Override
            protected void setValue(int number) {
                location.setRadius(number);
            }
            
            @Override
            protected void draw(MatrixStack matrices, GuiBase gui, boolean selected) {
                super.draw(matrices, gui, selected);
                
                gui.drawString(matrices, gui.getLinesFromText(Translator.translated("hqm.locationMenu.negRadius"), 0.7F, 130), BOX_X, BOX_Y + BOX_OFFSET * 5 + TEXT_OFFSET, 0.7F, 0x404040);
            }
        });
        
        
        buttons.add(new LargeButton("hqm.locationMenu.location", 100, 20) {
            @Override
            public boolean isEnabled(GuiBase gui, PlayerEntity player) {
                return true;
            }
            
            @Override
            public boolean isVisible(GuiBase gui, PlayerEntity player) {
                return true;
            }
            
            @Override
            public void onClick(GuiBase gui, PlayerEntity player) {
                location.setX((int) player.getX());
                location.setY((int) player.getY());
                location.setZ((int) player.getZ());
                location.setDimension(player.world.getDimensionRegistryKey());
                for (TextBoxGroup.TextBox textBox : textBoxes.getTextBoxes()) {
                    textBox.setTextAndCursor(gui, String.valueOf(((TextBoxNumber) textBox).getValue()));
                }
            }
        });
    }
    
    @Override
    protected void onArrowClick(boolean left) {
        if (left) {
            location.setVisible(QuestTaskLocation.Visibility.values()[(location.getVisible().ordinal() + QuestTaskLocation.Visibility.values().length - 1) % QuestTaskLocation.Visibility.values().length]);
        } else {
            location.setVisible(QuestTaskLocation.Visibility.values()[(location.getVisible().ordinal() + 1) % QuestTaskLocation.Visibility.values().length]);
        }
    }
    
    @Override
    protected String getArrowText() {
        return location.getVisible().getName();
    }
    
    @Override
    protected String getArrowDescription() {
        return location.getVisible().getDescription();
    }
    
    @Override
    public void save(GuiBase gui) {
        task.setLocation(id, location, player);
    }
    
    private abstract class TextBoxNumberNegative extends GuiEditMenuExtended.TextBoxNumber {
        public TextBoxNumberNegative(GuiQuestBook gui, int id, String title) {
            super(gui, id, title);
        }
        
        @Override
        protected boolean isNegativeAllowed() {
            return true;
        }
    }
}
