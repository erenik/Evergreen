/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

/**
 *
 * @author Emil
 */
public enum EGErrorType {
    BadRequest("BadRequest"),
    ParseError("ParseError"),
    BadPassword("BadPassword"),
    ;    
    EGErrorType(String errType)
    {
        text = errType;
    }
    String text;
}
