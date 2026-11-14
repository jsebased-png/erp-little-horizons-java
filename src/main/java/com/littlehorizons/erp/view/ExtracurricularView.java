package com.littlehorizons.erp.view;

import com.littlehorizons.erp.view.console.ConsoleInput;
import com.littlehorizons.erp.view.console.ConsolePrinter;
import com.littlehorizons.erp.view.console.Menu;

/**
 * Menu "4. Actividades Extracurriculares": sends the user to clubs or events.
 */
// SOLID: SRP – navigation only
public class ExtracurricularView {

    private final ClubView clubView;
    private final EventView eventView;
    private final ConsoleInput input;
    private final ConsolePrinter printer;

    public ExtracurricularView(ClubView clubView, EventView eventView, ConsoleInput input, ConsolePrinter printer) {
        this.clubView = clubView;
        this.eventView = eventView;
        this.input = input;
        this.printer = printer;
    }

    public void menu() {
        new Menu("ACTIVIDADES EXTRACURRICULARES", "Volver al menú principal", input, printer)
                .addSubmenu("Clubes", clubView::menu)
                .addSubmenu("Eventos", eventView::menu)
                .run();
    }
}
