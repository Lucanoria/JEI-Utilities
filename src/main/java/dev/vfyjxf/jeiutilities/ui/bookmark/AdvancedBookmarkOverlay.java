package dev.vfyjxf.jeiutilities.ui.bookmark;

import dev.vfyjxf.jeiutilities.config.JeiUtilitiesConfig;
import dev.vfyjxf.jeiutilities.config.KeyBindings;
import dev.vfyjxf.jeiutilities.jei.ingredient.RecipeInfo;
import dev.vfyjxf.jeiutilities.ui.common.GuiInputHandler;
import dev.vfyjxf.jeiutilities.ui.recipe.RecipePreviewWidget;
import dev.vfyjxf.jeiutilities.jei.JeiUtilitiesPlugin;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.bookmarks.BookmarkList;
import mezz.jei.gui.GuiHelper;
import mezz.jei.gui.GuiScreenHelper;
import mezz.jei.gui.elements.GuiIconToggleButton;
import mezz.jei.gui.overlay.IngredientGridWithNavigation;
import mezz.jei.gui.overlay.bookmarks.BookmarkOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;
import java.awt.Rectangle;
import java.util.Set;

import static mezz.jei.gui.overlay.IngredientGrid.INGREDIENT_HEIGHT;
import static mezz.jei.gui.overlay.IngredientGrid.INGREDIENT_WIDTH;

@SuppressWarnings("unused")
public class AdvancedBookmarkOverlay extends BookmarkOverlay {

    private static final int BUTTON_SIZE = 20;

    private final IngredientGridWithNavigation contents;
    private final GuiIconToggleButton recordConfigButton;
    private final BookmarkInputHandler inputHandler;

    private RecipeInfo<?, ?> infoUnderMouse;
    private RecipePreviewWidget recipeLayout;

    public static BookmarkOverlay create(BookmarkList bookmarkList, GuiHelper guiHelper, GuiScreenHelper guiScreenHelper) {
        if (JeiUtilitiesConfig.getRecordRecipes()) {
            return new AdvancedBookmarkOverlay(bookmarkList, guiHelper, guiScreenHelper);
        } else {
            return new BookmarkOverlay(bookmarkList, guiHelper, guiScreenHelper);
        }
    }

    private AdvancedBookmarkOverlay(BookmarkList bookmarkList, GuiHelper guiHelper, GuiScreenHelper guiScreenHelper) {
        super(bookmarkList, guiHelper, guiScreenHelper);
        this.recordConfigButton = RecordConfigButton.create(this);
        this.contents = ObfuscationReflectionHelper.getPrivateValue(BookmarkOverlay.class, this, "contents");
        this.inputHandler = BookmarkInputHandler.getInstance();
    }

    @Override
    public void updateBounds(@Nonnull Rectangle area, @Nonnull Set<Rectangle> guiExclusionAreas) {
        super.updateBounds(area, guiExclusionAreas);
        Rectangle rectangle = new Rectangle(area);
        rectangle.x = contents.getArea().x;
        rectangle.width = contents.getArea().width;
        this.recordConfigButton.updateBounds(new Rectangle(
                rectangle.x + BUTTON_SIZE + 2,
                (int) Math.floor(rectangle.getMaxY()) - BUTTON_SIZE - 2,
                BUTTON_SIZE,
                BUTTON_SIZE
        ));
    }

    @Override
    public void drawScreen(@Nonnull Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(minecraft, mouseX, mouseY, partialTicks);
        this.recordConfigButton.draw(minecraft, mouseX, mouseY, partialTicks);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void drawTooltips(@Nonnull Minecraft minecraft, int mouseX, int mouseY) {
        boolean renderRecipe = false;
        boolean shouldRenderRecipe = false;
        int eventKey = Keyboard.getEventKey();
        boolean displayRecipe = KeyBindings.isKeyDown(KeyBindings.displayRecipe, false);
        boolean isTransferRecipe = KeyBindings.isKeyDown(KeyBindings.transferRecipe);
        boolean isTransferRecipeMax = KeyBindings.isKeyDown(KeyBindings.transferRecipeMax);
        if (displayRecipe | isTransferRecipe | isTransferRecipeMax) {
            Object ingredientUnderMouse = this.getIngredientUnderMouse();
            if (ingredientUnderMouse instanceof RecipeInfo) {
                RecipeInfo recipeInfo = (RecipeInfo) ingredientUnderMouse;
                shouldRenderRecipe = true;
                RecipePreviewWidget recipeLayout;
                if (this.infoUnderMouse == recipeInfo) {
                    recipeLayout = this.recipeLayout;
                } else {
                    this.infoUnderMouse = recipeInfo;

                    recipeLayout = RecipePreviewWidget.createLayout(recipeInfo, mouseX, mouseY);
                    this.recipeLayout = recipeLayout;
                }

                if (recipeLayout != null && displayRecipe) {
                    updatePosition(mouseX, mouseY);
                    recipeLayout.drawRecipe(minecraft, mouseX, mouseY);
                    renderRecipe = true;
                }

            }
        }

        if (!(GuiInputHandler.isContainerTextFieldFocused() || JeiUtilitiesPlugin.ingredientListOverlay.hasKeyboardFocus())) {
            if (isTransferRecipe || isTransferRecipeMax) {
                if (shouldRenderRecipe && recipeLayout != null && recipeLayout.getTransferError() != null) {
                    if (!renderRecipe) {
                        recipeLayout.drawRecipe(minecraft, mouseX, mouseY);
                        renderRecipe = true;
                    }
                    recipeLayout.showError(minecraft, mouseX, mouseY);
                }
            }
        }

        if (!renderRecipe) {
            super.drawTooltips(minecraft, mouseX, mouseY);
            this.recordConfigButton.drawTooltips(minecraft, mouseX, mouseY);
        }

        if (inputHandler.getDraggedElement() != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 0.0F, 200.0F);
            IIngredientRenderer ingredientRenderer = inputHandler.getDraggedElement().getIngredientRenderer();
            ingredientRenderer.render(minecraft, mouseX, mouseY, inputHandler.getDraggedElement().getIngredient());
            GlStateManager.popMatrix();
        }

    }

    private void updatePosition(int mouseX, int mouseY) {
        if (this.recipeLayout != null) {
            int x = this.recipeLayout.getPosX();
            int y = this.recipeLayout.getPosY();
            Rectangle area = new Rectangle(x - INGREDIENT_WIDTH, y - INGREDIENT_WIDTH, INGREDIENT_WIDTH * 2, INGREDIENT_HEIGHT * 2);
            if (!area.contains(mouseX, mouseY)) {
                this.recipeLayout.setPosition(mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean handleMouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean result = super.handleMouseClicked(mouseX, mouseY, mouseButton);

        if (recordConfigButton.isMouseOver(mouseX, mouseY)) {
            return recordConfigButton.handleMouseClick(mouseX, mouseY);
        }

        return result;
    }

    public RecipeInfo<?, ?> getInfoUnderMouse() {
        return infoUnderMouse;
    }

    public RecipePreviewWidget getRecipeLayout() {
        return recipeLayout;
    }

    public void setInfoUnderMouse(RecipeInfo<?, ?> infoUnderMouse) {
        this.infoUnderMouse = infoUnderMouse;
    }

    public void setRecipeLayout(RecipePreviewWidget recipeLayout) {
        this.recipeLayout = recipeLayout;
    }

}
