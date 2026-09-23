package org.example.Twilio;

import com.twilio.Twilio;


public class TwilioConfig {

  private String accountSid;


  private String authToken;

  public void init() {
    Twilio.init(accountSid, authToken);
  }
}