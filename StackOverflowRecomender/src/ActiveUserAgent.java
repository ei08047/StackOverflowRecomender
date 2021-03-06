import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

import java.io.*;

import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

public class ActiveUserAgent extends Agent {
    private int nResponders;
    private int id;
    private final int desiredSetSize = 10;


    public ActiveUserAgent() {
        System.out.println("ENTERED ActiveUserAgent");

    }


    protected void setup() {

        //TODO: pass user id as argument

        Object[] args = this.getArguments();

        if(args != null && args.length > 0) {
            id = Integer.parseInt((String)args[0]);
            System.out.println("Hello! im a active user with id " + id);
            this.nResponders = args.length - 1 ;
            System.out.println("Trying to delegate dummy-action to one out of " + this.nResponders + " responders.");
            ACLMessage msg = new ACLMessage(3);


            for(int i = 1; i < args.length; i++) {
                msg.addReceiver(new AID((String)args[i], false));
            }
            int numRecSet = 5;
            msg.setProtocol("fipa-contract-net");
            msg.setReplyByDate(new Date(System.currentTimeMillis() + 100000L));
            msg.setContent( id + " " +  numRecSet);
            this.addBehaviour(new ContractNetInitiator(this, msg) {
                protected void handlePropose(ACLMessage propose, Vector v) {
                    System.out.println("Agent " + propose.getSender().getName() + " proposed \n\n" + propose.getContent());
                }

                protected void handleRefuse(ACLMessage refuse) {
                    System.out.println("Agent " + refuse.getSender().getName() + " refused");
                }

                protected void handleFailure(ACLMessage failure) {
                    if(failure.getSender().equals(this.myAgent.getAMS())) {
                        System.out.println("Responder does not exist");
                    } else {
                        System.out.println("Agent " + failure.getSender().getName() + " failed");
                    }

                    ActiveUserAgent.this.nResponders--;
                }

                protected void handleAllResponses(Vector responses, Vector acceptances) {
                    if(responses.size() < ActiveUserAgent.this.nResponders) {
                        System.out.println("Timeout expired: missing " + (ActiveUserAgent.this.nResponders - responses.size()) + " responses");
                    }

                    int bestProposal = -1;
                    AID bestProposer = null;
                    ACLMessage accept = null;
                    Enumeration e = responses.elements();

                    while(e.hasMoreElements()) {
                        ACLMessage msg = (ACLMessage)e.nextElement();
                        if(msg.getPerformative() == 11) {
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(15);
                            acceptances.addElement(reply);
                            accept=reply;
							//how to compare the proposals 
							//
                            /*
                            int proposal = Integer.parseInt(msg.getContent());
                            if(proposal > bestProposal) {
                                bestProposal = proposal;
                                bestProposer = msg.getSender();
                                accept = reply;
                            }
                            */
                        }
                    }

                    if(accept != null) {
                        //System.out.println("Accepting proposal " + bestProposal + " from responder " + bestProposer.getName());
                        accept.setPerformative(0);
                    }

                }

				
                protected void handleInform(ACLMessage inform) {
                    System.out.println("Agent " + inform.getSender().getName() + " successfully performed the requested action");
                    System.out.println("content: "+inform.getContent());
                }
            });
        } else {
            System.out.println("No responder specified.");
        }

    }
}
