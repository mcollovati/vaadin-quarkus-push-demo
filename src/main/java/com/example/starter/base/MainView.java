package com.example.starter.base;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

/**
 * The main view contains a button and a click listener.
 */
@Route("")
public class MainView extends VerticalLayout {

    private final transient ExecutorService service = Executors.newCachedThreadPool();
    private final Span tasksStatus = new Span();
    private final AtomicInteger taskCounter = new AtomicInteger();
    private final AtomicInteger completedTasks = new AtomicInteger();
    private final VerticalLayout messagesLayout;

    public MainView() {
        setHeight("100%");
        H1 title = new H1("Websocket PUSH on Quarkus 11");
        add(title);

        updateTasksStatus();

        HorizontalLayout layout = new HorizontalLayout(
                new Button("Start background job", this::startBackgroundJob),
                tasksStatus
        );
        layout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        add(layout);

        messagesLayout = new VerticalLayout();
        messagesLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        Div panel = new Div(messagesLayout);
        panel.setSizeFull();
        add(panel);

        setFlexGrow(1.0, panel);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    }


    @Override
    protected void onDetach(DetachEvent detachEvent) {
        service.shutdown();
    }

    private void updateTaskProgress(ProgressBar bar) {
        double nextValue = bar.getValue() + 1;
        if (nextValue < bar.getMax()) {
            bar.setValue(nextValue);
        } else {
            bar.setValue(bar.getMax());
            bar.addThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
            completedTasks.incrementAndGet();
            updateTasksStatus();
        }
    }

    private void startBackgroundJob(ClickEvent<Button> event) {
        getUI().ifPresent(ui -> {
            int taskExecutionTime = ThreadLocalRandom.current().nextInt(5, 20);
            int taskId = taskCounter.incrementAndGet();
            ProgressBar progressBar = addTaskProgressBar(taskId, taskExecutionTime);
            Runnable onUpdate = () -> ui.access(() -> updateTaskProgress(progressBar));
            service.submit(new Task(taskExecutionTime, onUpdate));
            updateTasksStatus();
        });
    }

    private ProgressBar addTaskProgressBar(int taskId, int taskTimeout) {
        ProgressBar progressBar = new ProgressBar(0, taskTimeout, 0);
        if (taskTimeout > 10) {
            progressBar.addClassName("slow-task");
        }
        HorizontalLayout l = new HorizontalLayout();
        Span label = new Span(String.format("Task %d (completion time %d s)", taskId, taskTimeout));
        label.getElement().getStyle().set("white-space", "nowrap");
        l.add(label);
        l.addAndExpand(progressBar);
        messagesLayout.addComponentAtIndex(0, l);
        updateTasksStatus();
        return progressBar;
    }


    private void updateTasksStatus() {
        tasksStatus.setText(String.format("Completed %d of %d",
                completedTasks.get(), taskCounter.get()
        ));
    }

    private static class Task implements Runnable {
        private final long executionTime;
        private final Runnable updater;

        Task(long executionTime, Runnable updater) {
            this.executionTime = executionTime;
            this.updater = updater;
        }

        @Override
        public void run() {
            int count = 0;
            while (count++ < executionTime) {
                // Sleep to emulate background work
                try {
                    Thread.sleep(1000);
                    updater.run();
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
    }
}
