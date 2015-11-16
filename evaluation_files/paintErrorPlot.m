%#paints the errorplot with two dimension including the error for two dimensions
%
%#RES is the matrix containing the Results to be plottet, every ROW is one scenario (result entity), every two COLUMNS is mean and std
%# ---------- ONLY first 4 COLOUMNS ARE BEING USED!!!! ------------
%#example:  meanX1 stdX1 meanY1 stdY1
%#          meanX2 stdX2 meanY2 stdY2
%
%# NAMES holds the names for the results - each row is a new scenario/result name
function paintErrorPlot(RES, NAMES, Normalized, scenname, paintNames)
    if(Normalized)
        xbar_max = RES(:,1) + RES(:,2);
        ybar_max = RES(:,3) + RES(:,4);
                                                
        normFactorX = norm(RES(:,1), inf);
        normFactorY = norm(RES(:,3), inf);

        xbar_norm = RES(:,1)/normFactorX;
        ybar_norm = RES(:,3)/normFactorY;
        
        erbarx_norm = RES(:,2)/normFactorX;
        erbary_norm = RES(:,4)/normFactorY;
    else
        xbar_norm = RES(:,1);
        ybar_norm = RES(:,3);
        erbarx_norm = RES(:,2);
        erbary_norm = RES(:,4);
    end
    
    plot(xbar_norm, ybar_norm, '^k');
    %line([xbar_norm xbar_norm], [(ybar_norm-erbary_norm) (ybar_norm+erbary_norm)], 'Color', 'black')
    %line([(xbar_norm-erbarx_norm) (xbar_norm+erbarx_norm)], [ybar_norm ybar_norm], 'Color', 'black')
    
    for nr = 1:numel(xbar_norm)
        line([xbar_norm(nr) xbar_norm(nr)], [(ybar_norm(nr)-erbary_norm(nr)) (ybar_norm(nr)+erbary_norm(nr))], 'Color', 'black')
        line([(xbar_norm(nr)-erbarx_norm(nr)) (xbar_norm(nr)+erbarx_norm(nr))], [ybar_norm(nr) ybar_norm(nr)], 'Color', 'black')
    end
   
    if(paintNames)
        text(xbar_norm, ybar_norm+0.03, NAMES);
    end
%     text(xbar_norm(2), ybar_norm(2)+0.03, 'P BC');
%     text(xbar_norm(3), ybar_norm(3)+0.03, 'A ST');
%     text(xbar_norm(4), ybar_norm(4)+0.03, 'P ST');
%     text(xbar_norm(5), ybar_norm(5)+0.03, 'A SM');
%     text(xbar_norm(6), ybar_norm(6)+0.03, 'P SM');
    title(scenname)
    xlabel('Communication');
    ylabel('Total Utility');
    saveas(gcf, strcat('figures/erperf', scenname(1:numel(scenname)-1), '.fig'), 'fig');
end