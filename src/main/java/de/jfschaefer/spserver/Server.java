package de.jfschaefer.spserver;

import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * A simple server receiving JSON requests and sending JSON replies.
 * The query should contain sentences, the reply will contain nlp data
 * obtained using the Stanford NLP Tools.
 * Created by jfs on 7/13/15.
 */

public class Server {
    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_PARSER_ERROR = 1;
    public static final int STATUS_INVALID_QUERY = 2;
    LexicalizedParser lparser = null;
    TreebankLanguagePack tlpack = null;
    GrammaticalStructureFactory gsfactory = null;

    public Server(int portnumber) throws IOException {
        System.out.println("Starting server on port " + portnumber);
        ServerSocket serverSocket = new ServerSocket(portnumber);
        JSONParser parser = new JSONParser();
        lparser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
        tlpack = lparser.treebankLanguagePack();
        if (tlpack.supportsGrammaticalStructures()) {
            gsfactory = tlpack.grammaticalStructureFactory();
        } else {
            System.err.println("de.jfschaefer.spserver.Server: " +
                    "TreebankLanguagePack doesn't support grammatical structures");
        }

        while (true) {
            String line;
            Socket connectionSocket = serverSocket.accept();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(connectionSocket.getOutputStream());
            //while ((line = reader.readLine()) != null) {
            {
                line = reader.readLine();
                System.out.println("Got a query: " + line);
                try {
                    JSONObject query = (JSONObject)parser.parse(line);   //TODO: Parse from reader directly
                    sendReply(writer, query);
                } catch (ParseException ex) {
                    System.err.println("de.jfschaefer.spserver.Server: Couldn't parse query");
                    System.err.println("Error at position " + parser.getPosition());
                    ex.printStackTrace();
                    sendError(writer, STATUS_PARSER_ERROR,
                              "Couldn't parse query at position " + parser.getPosition());
                }
            }
        }
    }

    private void sendReply(Writer writer, JSONObject query) throws IOException {
        if (!query.containsKey("requests")) {
            System.err.println("de.jfschaefer.spserver.Server: Invalid query");
            System.err.println("Missing key \"request\"");
            sendError(writer, STATUS_INVALID_QUERY, "Missing key \"requests\"");
            return;
        }
        JSONArray requests = (JSONArray)query.get("requests");
        boolean getPOS = false;
        if (requests.contains("pos")) {
            getPOS = true;
        }

        if (!query.containsKey("sentences")) {
            System.err.println("de.jfschaefer.spserver.Server: Invalid query");
            System.err.println("Missing key \"sentences\"");
            sendError(writer, STATUS_INVALID_QUERY, "Missing key \"sentences\"");
            return;
        }
        JSONArray sentences = (JSONArray)query.get("sentences");

        JSONObject reply = new JSONObject();
        JSONArray results = new JSONArray();
        for (Object sentence : sentences) {
            JSONObject result = new JSONObject();
            if (!(sentence instanceof JSONArray)) {
                System.err.println("de.jfschaefer.spserver.Server: Invalid query");
                System.err.println("Sentence not instance of JSONArray");
                sendError(writer, STATUS_INVALID_QUERY, "Sentence not instance of JSONArray");
                return;
            }

            ArrayList<Word> goodSentence = new ArrayList<Word>();
            for (Object word : (JSONArray)sentence) {
                if (!(word instanceof String)) {
                    System.err.println("de.jfschaefer.spserver.Server: Invalid query");
                    System.err.println("Word isn't instance of String");
                    sendError(writer, STATUS_INVALID_QUERY, "Word isn't instance of String");
                    return;
                }
                goodSentence.add(new Word((String) word));
            }
            if (getPOS) {
                Tree parse = lparser.apply(goodSentence);
                JSONArray postags = new JSONArray();
                for (Tree leaf : parse.getLeaves()) {
                    // the leaf is the word, its parent the POS (and everything above are phrase things)
                    postags.add(leaf.parent(parse).label().value());
                }
                result.put("pos", postags);
            }
            results.add(result);
        }
        reply.put("results", results);
        reply.put("status", STATUS_SUCCESS);
        reply.put("message", "everything okay");
        writer.write(reply.toJSONString());
        writer.flush();
        writer.close();
    }

    private void sendError(Writer writer, int errorCode, String message) throws IOException {
        JSONObject reply = new JSONObject();

        reply.put("status", 1);
        reply.put("message", message);

        writer.write(reply.toJSONString());
        writer.flush();
        writer.close();
    }
}
