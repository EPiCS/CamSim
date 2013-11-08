%#plots the distribution with two dimension without the error for two dimensions. error is needed for normalization! (and correct culculation)
%
%#RES is the matrix containing the Results to be plottet, every ROW is one scenario (result entity), every two COLUMNS is mean and std
%# ---------- ONLY first 4 COLOUMNS ARE BEING USED!!!! ------------
%#example:  meanX1 stdX1 meanY1 stdY1
%#          meanX2 stdX2 meanY2 stdY2
%
%# NAMES holds the names for the results - each row is a new scenario/result name
%# NormFactor is the factor to normalize values - put 1 if no normalization is desired
function paintDistPlot(RES, NAMES, NormFactor, scenname, paintNames, paintHomos, fileext)
    
    xbar_norm = RES(:,1)/NormFactor(1);
    ybar_norm = RES(:,3)/NormFactor(2);
    erbarx_norm = RES(:,2)/NormFactor(1);
    erbary_norm = RES(:,4)/NormFactor(2);

    if(paintHomos)
        allx = xbar_norm;
        ally = ybar_norm;
        erallx = erbarx_norm;
        erally = erbary_norm;
        allnames = NAMES;
        
        NAMES = zeros(0,0);
        erbarx_norm = zeros(0,0);
        erbary_norm = zeros(0,0);
        xbar_norm = zeros(0,0);
        ybar_norm = zeros(0,0);
        
        for nr = 1:numel(allnames(:,1))
            if(~IsHomogeneous(allnames(nr, :)))
               NAMES = [NAMES; allnames(nr,:)];
               xbar_norm = [xbar_norm; allx(nr, :)];
               ybar_norm = [ybar_norm; ally(nr, :)];
            end
        end
    end
    
    hold on
    plot(xbar_norm, ybar_norm, 'xk', 'MarkerSize', 14, 'LineWidth', 2);
    
    title(scenname)
    xlabel('Auctions');
    ylabel('Confidence');
    saveas(gcf, strcat('figures/perf', scenname(1:numel(scenname)-1), '.', fileext), fileext);
    
%     for R = 1:numel(xbar_norm)
%         line([xbar_norm(R) xbar_norm(R)], [(ybar_norm(R)-erbary_norm(R)) (ybar_norm(R)+erbary_norm(R))], 'Color', 'black')
%         line([(xbar_norm(R)-erbarx_norm(R)) (xbar_norm(R)+erbarx_norm(R))], [ybar_norm(R) ybar_norm(R)], 'Color', 'black')
%     end
%     
    if(paintNames)
        text(xbar_norm, ybar_norm+0.003, NAMES);
        saveas(gcf, strcat('figures/perf', scenname(1:numel(scenname)-1), '_names.', fileext), fileext);
    end
    
    xLimits = get(gca,'XLim');
    yLimits = get(gca,'YLim');

    axis([xLimits(1) 1.02 yLimits(1) 1.002]);
%     text(xbar_norm(2), ybar_norm(2)+0.03, 'P BC');
%     text(xbar_norm(3), ybar_norm(3)+0.03, 'A ST');
%     text(xbar_norm(4), ybar_norm(4)+0.03, 'P ST');
%     text(xbar_norm(5), ybar_norm(5)+0.03, 'A SM');
%     text(xbar_norm(6), ybar_norm(6)+0.03, 'P SM');
    hold off
end