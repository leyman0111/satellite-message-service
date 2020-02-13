package com.sokolmeteo.satellitemessageservice.tcp;

import com.sokolmeteo.satellitemessageservice.dto.IridiumMessage;
import com.sokolmeteo.satellitemessageservice.dto.Payload;
import com.sokolmeteo.satellitemessageservice.repo.IridiumMessageRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.SimpleDateFormat;
import java.util.*;

public class TCPClientService {
    private final TCPClient client;
    private final IridiumMessageRepository repository;

    public TCPClientService(TCPClient client, IridiumMessageRepository repository) {
        this.client = client;
        this.repository = repository;
    }

    @Scheduled(fixedDelay = 60000)
    public void export() {
        System.out.println("Export messages");
        List<IridiumMessage> iridiumMessages = repository.findByErrorCounterAndSent(0, false);
        if (iridiumMessages.size() > 0) {
            Map<String, List<IridiumMessage>> groupedMessages = groupByImei(iridiumMessages);
            for (String imei : groupedMessages.keySet()) {
                List<IridiumMessage> messages = groupedMessages.get(imei);
                boolean response = client.sendMessage(imei, getBlackMessage(messages));
                if (response) {
                    for (IridiumMessage message : messages) {
                        message.setSent(true);
                    }
                    repository.saveAll(messages);
                }
            }
            System.out.println("Export is successful");
        } else System.out.println("Nothing to export");
    }

    private Map<String, List<IridiumMessage>> groupByImei(List<IridiumMessage> messages) {
        Map<String, List<IridiumMessage>> grouppedMessages = new HashMap<>();
        for (IridiumMessage message : messages) {
            if (grouppedMessages.containsKey(message.getImei())) {
                grouppedMessages.get(message.getImei()).add(message);
            } else {
                List<IridiumMessage> messages1 = new ArrayList<>();
                messages1.add(message);
                grouppedMessages.put(message.getImei(), messages1);
            }
        }
        return grouppedMessages;
    }

    String getBlackMessage(List<IridiumMessage> messages) {
        StringBuilder sokolMessage = new StringBuilder("#B#");
        for (IridiumMessage message : messages) {
            Payload payload = getPayload(message.getPayload());
            sokolMessage.append(payload.getDate());
            sokolMessage.append(";");
            sokolMessage.append(payload.getTime());
            sokolMessage.append(";");
            sokolMessage.append(message.getLatitude());
            sokolMessage.append(";");
            sokolMessage.append(message.getLatitudeDirection().getLiteral());
            sokolMessage.append(message.getLongitude());
            sokolMessage.append(";");
            sokolMessage.append(message.getLongitudeDirection().getLiteral());
            sokolMessage.append("NA;NA;");
            sokolMessage.append(message.getHeight() != null ? message.getHeight() : "NA");
            sokolMessage.append(";");
            sokolMessage.append("0;NA;0;0;NA;NA;");
            sokolMessage.append("ER:1:");
            sokolMessage.append(payload.getErrors());
            sokolMessage.append(",TR:1:");
            sokolMessage.append(payload.getCount());
            sokolMessage.append(",Upow:2:");
            sokolMessage.append(payload.getVoltage1());
            sokolMessage.append(",ExtUpow:2:");
            sokolMessage.append(payload.getVoltage2());
            sokolMessage.append(",Temp:2:");
            sokolMessage.append(payload.getTemperature());
            sokolMessage.append(",PR:2:");
            sokolMessage.append(payload.getPressure());
            sokolMessage.append(",HM:1:");
            sokolMessage.append(payload.getMoisture());
            sokolMessage.append(",WV:2:");
            sokolMessage.append(payload.getWindSpeed());
            sokolMessage.append(",WD:1:");
            sokolMessage.append(payload.getWindDirection());
            sokolMessage.append(",MWV:2:");
            sokolMessage.append(payload.getWindFlaw());
            sokolMessage.append(",RN:2:");
            sokolMessage.append(payload.getPrecipitation());
            sokolMessage.append(",SR:2:");
            sokolMessage.append(payload.getSolarRadiation());
            sokolMessage.append(",SH:1:");
            sokolMessage.append(payload.getSnowDepth());
            sokolMessage.append(",HG:1:");
            sokolMessage.append(payload.getSoilMoisture());
            sokolMessage.append(",TG:1:");
            sokolMessage.append(payload.getSoilTemperature());
            sokolMessage.append("|");
        }
        sokolMessage.append("\r\n");
        return sokolMessage.toString();
    }

    private Payload getPayload(String string) {
        Payload payload = new Payload();
        StringTokenizer tokenizer = new StringTokenizer(string, "[ ]");

        byte[] byteDate = {Byte.parseByte(tokenizer.nextToken()),
                Byte.parseByte(tokenizer.nextToken()),
                Byte.parseByte(tokenizer.nextToken()),
                Byte.parseByte(tokenizer.nextToken())};
        Calendar date = getDate(byteDate);
        SimpleDateFormat dateFormatter = new SimpleDateFormat("ddMMyy");
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HHmmss");
        payload.setDate(dateFormatter.format(date.getTime()));
        payload.setTime(timeFormatter.format(date.getTime()));

        byte[] byteError = {Byte.parseByte(tokenizer.nextToken())};
        payload.setErrors((int) TCPServerUtils.byteArrayToLong(byteError, 0, 1));

        byte[] byteCount = {Byte.parseByte(tokenizer.nextToken())};
        payload.setCount((int) TCPServerUtils.byteArrayToLong(byteCount, 0, 1));

        byte[] byteVoltage1 = {Byte.parseByte(tokenizer.nextToken())};
        payload.setVoltage1(((float) TCPServerUtils.byteArrayToLong(byteVoltage1, 0, 1)) * 2 / 100);

        byte[] byteVoltage2 = {Byte.parseByte(tokenizer.nextToken())};
        payload.setVoltage2(((float) TCPServerUtils.byteArrayToLong(byteVoltage2, 0, 1)) / 16);

        byte[] byteTemperature = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        payload.setTemperature(((float) TCPServerUtils.byteArrayToInt(byteTemperature, 0, 2)) / 100);

        byte[] bytePressure = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        payload.setPressure(((float) TCPServerUtils.byteArrayToLong(bytePressure, 0, 2)) * 3 / 100);

        payload.setMoisture(Byte.parseByte(tokenizer.nextToken()));

        byte[] byteWindSpeed = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        payload.setWindSpeed(((float) TCPServerUtils.byteArrayToInt(byteWindSpeed, 0, 2)) / 100);

        byte[] byteWindDirection = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        payload.setWindDirection(TCPServerUtils.byteArrayToInt(byteWindDirection, 0, 2));

        byte[] byteWindFlaw = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        payload.setWindFlaw(((float) TCPServerUtils.byteArrayToInt(byteWindFlaw, 0, 2)) / 100);

        byte[] bytePrecipitation = {Byte.parseByte(tokenizer.nextToken())};
        payload.setPrecipitation(((float) TCPServerUtils.byteArrayToLong(bytePrecipitation, 0, 1)) / 10);

        byte[] byteSolarRadiation = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        payload.setSolarRadiation(((float) TCPServerUtils.byteArrayToInt(byteSolarRadiation, 0, 2)) / 10);

        byte[] byteSnowDepth = {Byte.parseByte(tokenizer.nextToken()), Byte.parseByte(tokenizer.nextToken())};
        payload.setSnowDepth(TCPServerUtils.byteArrayToInt(byteSnowDepth, 0, 2));

        payload.setSoilMoisture(Byte.parseByte(tokenizer.nextToken()));

        byte[] byteSoilTemperature = {Byte.parseByte(tokenizer.nextToken())};
        payload.setSoilTemperature(((float)TCPServerUtils.byteArrayToInt(byteSoilTemperature, 0, 1)) / 2);
        return payload;
    }

    private Calendar getDate(byte[] bytes) {
        long epochTime = TCPServerUtils.byteArrayToLong(bytes, 0, 4) * 1000L;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(epochTime);
        calendar.add(Calendar.HOUR, -3);
        return calendar;
    }
}
