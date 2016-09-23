/*
 * Copyright 2016 The Johns Hopkins University Applied Physics Laboratory LLC
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package patrone.david.dorset.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import edu.jhuapl.dorset.Application;
import edu.jhuapl.dorset.Request;
import edu.jhuapl.dorset.Response;
import edu.jhuapl.dorset.agents.Agent;
import edu.jhuapl.dorset.routing.SingleAgentRouter;

/**
 *
 */
public class SimpleClientUI extends JFrame implements ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = -5862459158927993214L;

    protected Application app;

    protected JTextField inputField;
    protected JLabel requestLabel;
    protected JTextArea responseArea;

    public SimpleClientUI(String title, Agent agent) {
        this(title, new Application(new SingleAgentRouter(agent)));
    }

    public SimpleClientUI(String title, Application app) {
        super(title);
        this.app = app;

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(new Dimension(600, 400));
        this.setLocationRelativeTo(null);

        inputField = new JTextField();
        inputField.addActionListener(this);
        JButton go = new JButton("Go");
        go.addActionListener(this);
        requestLabel = new JLabel(" ");
        responseArea = new JTextArea(8, 20);
        responseArea.setEditable(false);
        responseArea.setEnabled(false);
        responseArea.setLineWrap(true);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JLabel("Input:"), BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(go, BorderLayout.EAST);
        inputPanel.add(requestLabel, BorderLayout.SOUTH);

        this.getContentPane().add(inputPanel, BorderLayout.NORTH);
        this.getContentPane().add(new JScrollPane(responseArea), BorderLayout.CENTER);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String inputText = inputField.getText();
        Response r = app.process(new Request(inputText));
        responseArea.setText(r.getText());
        inputField.setText("");
        requestLabel.setText(inputText);
    }


    /**
     * @param args
     */
    public static void main(String[] args) {

    }

}
