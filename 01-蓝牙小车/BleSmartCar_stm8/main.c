#include "IOSTM8S103F3.h"
#define BIT(x)      (1<<(x))
#define IN1_HIGH    PC_ODR|=BIT(3)
#define IN2_HIGH    PC_ODR|=BIT(4)
#define IN3_HIGH    PC_ODR|=BIT(5)
#define IN4_HIGH    PC_ODR|=BIT(6)
#define IN1_LOW     PC_ODR&=~BIT(3)
#define IN2_LOW     PC_ODR&=~BIT(4)
#define IN3_LOW     PC_ODR&=~BIT(5)
#define IN4_LOW     PC_ODR&=~BIT(6)
void setVelocity(int,int,int,int);

int count=0;
int pwm_in1=0;
int pwm_in2=0;
int pwm_in3=0;
int pwm_in4=0;
int velocity=10;

//---  ΢�뼶��ʱ--------------------------   
void delay_us(void)   
{    
    asm("nop"); //һ��asm("nop")��������ʾ�������Դ���100ns   
    asm("nop");   
    asm("nop");   
    asm("nop");    
}   
  
//---- ���뼶��ʱ����-----------------------   
void delay_ms(unsigned int ms)
{
       unsigned char i;
       while(ms!=0)
       {
              for(i=0;i<250;i++)
              {}
              for(i=0;i<75;i++)
              {}
              ms--;
       }
}


void Init_Timer4(void)
{
    
    TIM4_CNTR=0; //������ֵ  
    TIM4_ARR=0xFA; //�Զ���װ�Ĵ���  250������125�ζ�ʱ1S  
    TIM4_PSCR=0x07; //Ԥ��Ƶϵ��Ϊ128  
    TIM4_EGR=0x01; //�ֶ�����һ�������¼�,����PSC��Ч       ע�⣬���ֶ�����  
      
    TIM4_IER=0x01; //�����¼��ж� ʹ��  
    TIM4_CR1=0x01; //ʹ�ܼ�ʱ����TIM4_CR0ֹͣ��ʱ��  

}

void Port_Config(void)
{
    PC_DDR|=BIT(3)|BIT(4)|BIT(5)|BIT(6);
    PC_CR1|=BIT(3)|BIT(4)|BIT(5)|BIT(6);
    PC_CR2|=0x00;
    
    //PB_DDR|=BIT(4)|BIT(5);
    //PB_CR1|=BIT(4)|BIT(5);
    //PB_CR2|=0x00;
}



void Init_UART1(void)
{
      UART1_CR1=0x00;
      UART1_CR2=0x00;
      UART1_CR3=0x00;
      // ���ò����ʣ�����ע�����¼��㣺
      // (1) ������дBRR2
      // (2) BRR1��ŵ��Ƿ�Ƶϵ���ĵ�11λ����4λ��
      // (3) BRR2��ŵ��Ƿ�Ƶϵ���ĵ�15λ����12λ���͵�3λ
      // ����0λ
      // ������ڲ�����λ9600ʱ����Ƶϵ��=2000000/9600=208
      // ��Ӧ��ʮ��������Ϊ00D0��BBR1=0D,BBR2=00

      UART1_BRR2=0x00;
      UART1_BRR1=0x0d;

      UART1_CR2=0x2c;//������գ����ͣ��������ж�
}

int main( void )
{
    Init_Timer4();
    Init_UART1();
    Port_Config();
    asm("rim");
    while(1){}
    return 0;
}

void setVelocity(int aVal,int bVal,int cVal,int dVal)
{
    pwm_in1=aVal;
    pwm_in2=bVal;
    pwm_in3=cVal;
    pwm_in4=dVal;
}

#pragma vector= UART1_R_OR_vector//0x19
__interrupt void UART1_R_OR_IRQHandler(void)
{
      unsigned char dat;
      unsigned char temp;
      dat=UART1_DR;
      //UART1_sendchar(ch+1);
      switch(dat)
      {
      case 0x00: setVelocity(0,0,0,0);break;
      case 0x01: setVelocity(0,10,0,10);break;//ǰ
      case 0x02: setVelocity(10,0,10,0);break;//��
      case 0x03: setVelocity(0,10,0,4);break;//��
      case 0x04: setVelocity(0,4,0,10);break;//you
      //default:temp=dat%100;
      
      }
      return;
}

#pragma vector=TIM4_OVR_UIF_vector
__interrupt void TIM4_OVR_UIF_IRQHandler(void)
{
      count++;
      TIM4_SR=0x00;//����жϱ�־
      //������·pwm
      if(count<pwm_in1)
        IN1_HIGH;
      else
        IN1_LOW;
      
      if(count<pwm_in2)
        IN2_HIGH;
      else
        IN2_LOW;
      
      if(count<pwm_in3)
        IN3_HIGH;
 
      else
        IN3_LOW;
      
      if(count<pwm_in4)
        IN4_HIGH;
      else
        IN4_LOW;
      
      if(count == 10)
          count=0;
}
