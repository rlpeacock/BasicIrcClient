package justdust.basicirc;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class MessageTest {

    @Test
    public void testParsePing() {
        String ping = "PING :asimov.freenode.net";
        Message msg = Message.parseMessage(ping);
        assertNull(msg.getSender());
        assertEquals(msg.getType(), MessageType.PING);
        assertEquals(msg.getParam(0), ":asimov.freenode.net");
    }

    @Test
    public void testParseJoin() {
        String join = ":nick!~nick@pool-71-184-232-124.some.thing.verizon.net JOIN #test";
        Message msg = Message.parseMessage(join);
        assertEquals(msg.getSender(), ":nick!~nick@pool-71-184-232-124.some.thing.verizon.net");
        assertEquals(msg.getType(), MessageType.JOIN);
        assertEquals(msg.getParam(0), "#test");
    }

    @Test
    public void testParsePart() {
        String part = ":nick!~nick@unaffiliated/user PART #test :\"This is my quote. I'm using it as my part message.\"";
        Message msg = Message.parseMessage(part);
        assertEquals(msg.getSender(), ":nick!~nick@unaffiliated/user");
        assertEquals(msg.getType(), MessageType.PART);
        assertEquals(msg.getParam(0), "#test");
        assertEquals(msg.getParam(1), "\"This is my quote. I'm using it as my part message.\"");
    }

    @Test
    public void testGetDisplayName() {
        String name = Message.getNickFromName(":nick!~nick@unaffiliated/user");
        assertEquals(name, "nick");
        name = Message.getNickFromName(":nick!~nick@pool-71-184-232-124.some.thing.verizon.net");
        assertEquals(name, "nick");
    }

    @Test
    public void testFormatAsSpeech() {
        String say = ":nick!~nick@unaffiliated/user PRIVMSG #test :this is the message";
        Message msg = Message.parseMessage(say);
        assertEquals(Message.formatAsSpeech(msg), "[#test] nick: this is the message");
    }

    @Test
    public void testFormatAsSpeechEmote() {
        String emote = ":nick!~nick@unaffiliated/user PRIVMSG #test :ACTION does an emote";
        Message msg = Message.parseMessage(emote);
        assertEquals(Message.formatAsSpeech(msg), "[#test] nick does an emote");
    }

    @Test
    public void testCreateSay() {
        Message msg = Message.message(MessageType.PRIVMSG, "#test", ":this is my message");
        assertEquals(msg.toString(), "PRIVMSG #test :this is my message\r\n");
    }
}
