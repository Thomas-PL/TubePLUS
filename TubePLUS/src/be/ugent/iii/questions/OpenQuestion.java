/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package be.ugent.iii.questions;

/**
 *
 * @author Thomas
 */
public class OpenQuestion extends Question{
    
    private String answer;
    
    public OpenQuestion(){
        setDescription("Opmerkingen: ");
        answer = "";
    }

    @Override
    public boolean betweenBorders(double lowerlimit, double upperlimit) {
        return true;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    
    
}
