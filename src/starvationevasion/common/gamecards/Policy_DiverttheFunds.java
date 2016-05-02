package starvationevasion.common.gamecards;

import starvationevasion.common.EnumRegion;

public class Policy_DiverttheFunds extends GameCard
{
  
  public static String TITLE = "Divert the Funds";
  
  public static String TEXT = 
      "Discard your current hand and gain " + 
      "14 million dollars";
  
  /**
   * {@inheritDoc}
   */
  @Override
  public String getTitle() {return TITLE;}
  
  /**
   * {@inheritDoc}
   */
  @Override
  public String getGameText() {return TEXT;}
  
}
